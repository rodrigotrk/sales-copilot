package app.salescopilot.importing;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Roda a extração de texto num processo Java filho, com memória e tempo limitados (AD-063). Uma bomba de
 * compressão ou um documento que trave o parser encerra só o filho, e a tarefa termina em `import_failed`
 * sem afetar o servidor. O texto extraído volta pela saída padrão do filho e fica só na memória (AC 87).
 */
@Component
class ExtractionProcess {

    private static final Logger log = LoggerFactory.getLogger(ExtractionProcess.class);
    private static final Set<String> KNOWN_CODES = Set.of("import_failed", "text_too_long");

    /** O maior resultado possível: XHTML e texto no teto, em UTF-8 (até 4 bytes por caractere), mais o cabeçalho. */
    private static final int MAX_RESULT_BYTES = 4 * (TextExtraction.MAX_FOR_MODEL + TextExtraction.MAX_TEXT + 1) + 64;
    private static final int MAX_LOG_BYTES = 4_000;

    private final Path importDirectory;
    private final String heap;
    private final Duration timeout;

    ExtractionProcess(@Value("${app.import.directory}") Path importDirectory,
            @Value("${app.import.extraction-heap:512m}") String heap,
            @Value("${app.import.extraction-timeout:120s}") Duration timeout) {
        this.importDirectory = importDirectory;
        this.heap = heap;
        this.timeout = timeout;
    }

    DocumentText.Extracted extract(Path file, String expectedType) throws ImportFailure {
        Path scratch = null;
        Process process = null;
        try {
            // Os arquivos temporários do Tika (cópias do documento, nunca o texto) ficam numa pasta dentro da
            // de importação: se o servidor cair no meio, a varredura de órfãos a apaga.
            scratch = importDirectory.resolve("extracao-" + java.util.UUID.randomUUID());
            Files.createDirectories(scratch);
            List<String> command = new ArrayList<>(List.of(javaBinary(), "-Xmx" + heap, "-XX:+ExitOnOutOfMemoryError",
                    "-Djava.awt.headless=true", "-Djava.io.tmpdir=" + scratch));
            command.addAll(launch());
            command.addAll(List.of(file.toAbsolutePath().toString(), expectedType));
            process = new ProcessBuilder(command).start();
            process.getOutputStream().close();
            // Saída e erro lidos ao mesmo tempo, com teto: um filho que escreva demais não trava nem enche a memória.
            var result = readBounded(process.getInputStream(), MAX_RESULT_BYTES);
            var errors = readBounded(process.getErrorStream(), MAX_LOG_BYTES);
            if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                process.destroyForcibly().waitFor(10, TimeUnit.SECONDS);
                log.warn("Extração de {} passou de {} e foi encerrada", file.getFileName(), timeout);
                throw new ImportFailure("import_failed");
            }
            Bounded output = result.get(10, TimeUnit.SECONDS);
            if (process.exitValue() != 0) {
                log.warn("Extração de {} terminou com código {}: {}", file.getFileName(), process.exitValue(),
                        new String(errors.get(10, TimeUnit.SECONDS).bytes(), StandardCharsets.UTF_8));
                throw new ImportFailure("import_failed");
            }
            if (output.truncated()) {
                throw new ImportFailure("text_too_long");
            }
            return decode(output.bytes());
        } catch (IOException | java.util.concurrent.ExecutionException | java.util.concurrent.TimeoutException e) {
            throw new ImportFailure("import_failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ImportFailure("import_failed", e);
        } finally {
            // Em qualquer saída, inclusive interrupção no desligamento, o filho não fica rodando sem tempo limite.
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
            if (scratch != null) {
                deleteQuietly(scratch);
            }
        }
    }

    private record Bounded(byte[] bytes, boolean truncated) {
    }

    /** Lê até `max` bytes e descarta o resto, para o filho nunca bloquear escrevendo. */
    private static java.util.concurrent.Future<Bounded> readBounded(InputStream in, int max) {
        var future = new java.util.concurrent.CompletableFuture<Bounded>();
        Thread.ofVirtual().start(() -> {
            try (in) {
                byte[] kept = in.readNBytes(max);
                boolean truncated = in.transferTo(java.io.OutputStream.nullOutputStream()) > 0;
                future.complete(new Bounded(kept, truncated));
            } catch (IOException e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    /** O protocolo de {@link IsolatedExtraction}: "ok" com o XHTML e o texto, ou "error" com o código. */
    private static DocumentText.Extracted decode(byte[] bytes) throws IOException, ImportFailure {
        var in = new java.io.DataInputStream(new java.io.ByteArrayInputStream(bytes));
        String status = in.readUTF();
        if (IsolatedExtraction.ERROR.equals(status)) {
            String code = in.readUTF();
            throw new ImportFailure(KNOWN_CODES.contains(code) ? code : "import_failed");
        }
        String forModel = text(in, TextExtraction.MAX_FOR_MODEL);
        String plain = text(in, TextExtraction.MAX_TEXT + 1);
        return new DocumentText.Extracted(forModel, plain);
    }

    private static String text(java.io.DataInputStream in, int maxChars) throws IOException, ImportFailure {
        int length = in.readInt();
        if (length < 0 || length > 4L * maxChars) {
            throw new ImportFailure("text_too_long");
        }
        String text = new String(in.readNBytes(length), StandardCharsets.UTF_8);
        if (text.length() > maxChars) {
            throw new ImportFailure("text_too_long");
        }
        return text;
    }

    private static String javaBinary() {
        return ProcessHandle.current().info().command()
                .orElse(Path.of(System.getProperty("java.home"), "bin", "java").toString());
    }

    /**
     * Inicia o filho com o mesmo classpath do servidor: no jar do Spring Boot, pelo PropertiesLauncher com
     * `loader.main`; fora dele (testes, IDE), direto pela classe.
     */
    private static List<String> launch() throws IOException {
        String classpath = System.getProperty("java.class.path");
        if (isBootJar(classpath)) {
            return List.of("-Dloader.main=" + IsolatedExtraction.class.getName(), "-cp", classpath,
                    "org.springframework.boot.loader.launch.PropertiesLauncher");
        }
        return List.of("-cp", classpath, IsolatedExtraction.class.getName());
    }

    private static boolean isBootJar(String classpath) throws IOException {
        if (classpath.contains(java.io.File.pathSeparator) || !classpath.endsWith(".jar")) {
            return false;
        }
        try (ZipFile jar = new ZipFile(classpath)) {
            return jar.getEntry("BOOT-INF/") != null || jar.getEntry("BOOT-INF/classes/") != null;
        }
    }

    private static void deleteQuietly(Path directory) {
        try (var paths = Files.walk(directory)) {
            paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
        } catch (IOException e) {
            log.debug("Pasta temporária da extração não foi apagada: {}", directory, e);
        }
    }
}
