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
 * sem afetar o servidor.
 */
@Component
class ExtractionProcess {

    private static final Logger log = LoggerFactory.getLogger(ExtractionProcess.class);
    private static final Set<String> KNOWN_CODES = Set.of("import_failed", "text_too_long");

    private final String heap;
    private final Duration timeout;

    ExtractionProcess(@Value("${app.import.extraction-heap:512m}") String heap,
            @Value("${app.import.extraction-timeout:120s}") Duration timeout) {
        this.heap = heap;
        this.timeout = timeout;
    }

    DocumentText.Extracted extract(Path file, String expectedType) throws ImportFailure {
        Path out = null;
        try {
            out = Files.createTempDirectory("extracao-");
            List<String> command = new ArrayList<>(List.of(javaBinary(), "-Xmx" + heap, "-XX:+ExitOnOutOfMemoryError",
                    "-Djava.awt.headless=true"));
            command.addAll(launch());
            command.addAll(List.of(file.toAbsolutePath().toString(), expectedType, out.toString()));
            Process process = new ProcessBuilder(command)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(out.resolve("stderr.txt").toFile())
                    .start();
            if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                process.destroyForcibly().waitFor(10, TimeUnit.SECONDS);
                log.warn("Extração de {} passou de {} e foi encerrada", file.getFileName(), timeout);
                throw new ImportFailure("import_failed");
            }
            if (process.exitValue() != 0) {
                log.warn("Extração de {} terminou com código {}: {}", file.getFileName(), process.exitValue(),
                        head(out.resolve("stderr.txt")));
                throw new ImportFailure("import_failed");
            }
            Path error = out.resolve("error.txt");
            if (Files.exists(error)) {
                String code = Files.readString(error, StandardCharsets.UTF_8).strip();
                throw new ImportFailure(KNOWN_CODES.contains(code) ? code : "import_failed");
            }
            return new DocumentText.Extracted(read(out.resolve("model.txt"), TextExtraction.MAX_FOR_MODEL),
                    read(out.resolve("plain.txt"), TextExtraction.MAX_TEXT + 1));
        } catch (IOException e) {
            throw new ImportFailure("import_failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ImportFailure("import_failed", e);
        } finally {
            if (out != null) {
                deleteQuietly(out);
            }
        }
    }

    /** Lê a saída do filho com teto: o filho já limita, e o servidor não confia nisso para alocar memória. */
    private static String read(Path file, int maxChars) throws IOException, ImportFailure {
        if (Files.size(file) > 4L * maxChars) {
            throw new ImportFailure("text_too_long");
        }
        String text = Files.readString(file, StandardCharsets.UTF_8);
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

    private static String head(Path file) {
        try (InputStream in = Files.newInputStream(file)) {
            return new String(in.readNBytes(2_000), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
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
