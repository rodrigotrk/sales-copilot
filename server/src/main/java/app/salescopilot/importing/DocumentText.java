package app.salescopilot.importing;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.tika.exception.WriteLimitReachedException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.apache.tika.sax.TeeContentHandler;
import org.apache.tika.sax.ToXMLContentHandler;
import org.apache.tika.sax.WriteOutContentHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Texto do documento (AD-027). O modelo recebe o XHTML do Tika, que mantém as tabelas do .docx; a
 * conferência de valores em dinheiro usa o texto puro.
 */
@Component
class DocumentText {

    record Extracted(String forModel, String plain) {
    }

    private static final int MAX_TEXT = ImportWorker.MAX_TEXT;
    /** Teto do XHTML enviado ao modelo: o texto do corpo com as tags das tabelas cabe com folga. */
    static final int MAX_FOR_MODEL = 2_000_000;

    private static final Pattern GOOGLE_DOC = Pattern.compile("^https://docs\\.google\\.com/document/d/([A-Za-z0-9_-]+)(/.*)?$");

    private final RestClient http;
    private final String googleDocsBaseUrl;

    DocumentText(RestClient.Builder builder, @Value("${app.import.google-docs-base-url}") String googleDocsBaseUrl) {
        var requests = new SimpleClientHttpRequestFactory();
        requests.setConnectTimeout(10_000);
        requests.setReadTimeout(60_000);
        this.http = builder.requestFactory(requests).build();
        this.googleDocsBaseUrl = googleDocsBaseUrl;
    }

    Extracted fromFile(Path file) throws ImportFailure {
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        try {
            if (name.endsWith(".txt") || name.endsWith(".md")) {
                String text = Files.readString(file, StandardCharsets.UTF_8);
                return new Extracted(text, text);
            }
            Extracted extracted = parseFile(file, name.endsWith(".pdf") ? PDF : DOCX);
            if (name.endsWith(".pdf") && extracted.plain().isBlank()) {
                throw new ImportFailure("pdf_without_text");
            }
            return extracted;
        } catch (IOException e) {
            throw new ImportFailure("import_failed", e);
        }
    }

    /**
     * Só aceita links do Google Docs; o servidor monta a URL de exportação, então o pedido não sai para
     * outro endereço que o gestor informe.
     */
    Extracted fromGoogleDocs(String url) throws ImportFailure {
        Matcher matcher = GOOGLE_DOC.matcher(url == null ? "" : url.strip());
        if (!matcher.matches()) {
            throw new ImportFailure("google_docs_unavailable");
        }
        URI export = URI.create(googleDocsBaseUrl + "/document/d/" + matcher.group(1) + "/export?format=docx");
        byte[] body;
        try {
            // Lê no máximo o limite de upload mais um byte: uma exportação enorme não é carregada inteira.
            body = http.get().uri(export).exchange((request, response) -> {
                if (!response.getStatusCode().is2xxSuccessful()) {
                    return null;
                }
                try (InputStream in = response.getBody()) {
                    return in.readNBytes(Math.toIntExact(ImportController.MAX_FILE_BYTES) + 1);
                }
            });
        } catch (RuntimeException e) {
            throw new ImportFailure("google_docs_unavailable", e);
        }
        if (body != null && body.length > ImportController.MAX_FILE_BYTES) {
            // Acima de 10 MB o texto passaria do limite de 100 mil caracteres (AC 77).
            throw new ImportFailure("text_too_long");
        }
        // Documento não compartilhado devolve a página de login do Google (HTML), não um .docx (zip "PK").
        if (body == null || body.length < 2 || body[0] != 'P' || body[1] != 'K') {
            throw new ImportFailure("google_docs_unavailable");
        }
        Path copy = null;
        try {
            // Em arquivo, para a conferência ler o diretório central como o parser.
            copy = Files.createTempFile("google-docs-", ".docx");
            Files.write(copy, body);
            return parseFile(copy, DOCX);
        } catch (IOException e) {
            throw new ImportFailure("import_failed", e);
        } finally {
            if (copy != null) {
                try {
                    Files.deleteIfExists(copy);
                } catch (IOException e) {
                    // A pasta temporária do sistema limpa o que sobrar.
                }
            }
        }
    }

    private static final String DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    private static final String PDF = "application/pdf";
    private static final String OOXML = "application/x-tika-ooxml";

    /**
     * O conteúdo precisa ser do tipo que a extensão promete: um zip enviado como .pdf não chega ao parser.
     * O .docx passa pela conferência de compressão antes.
     */
    private static Extracted parseFile(Path file, String expectedType) throws IOException, ImportFailure {
        String detected;
        try (TikaInputStream in = TikaInputStream.get(file)) {
            detected = new AutoDetectParser().getDetector().detect(in, new Metadata()).getBaseType().toString();
        }
        // O detector às vezes só reconhece o pacote OOXML genérico; o parser do .docx confere o resto.
        boolean matches = detected.equals(expectedType) || (expectedType.equals(DOCX) && detected.equals(OOXML));
        if (!matches) {
            throw new ImportFailure("import_failed");
        }
        if (expectedType.equals(DOCX)) {
            ZipBudget.check(file);
        }
        try (InputStream in = Files.newInputStream(file)) {
            return parse(in);
        }
    }

    private static Extracted parse(InputStream in) throws IOException, ImportFailure {
        ToXMLContentHandler xhtml = new ToXMLContentHandler();
        // Para de ler quando o texto do corpo passa do limite: um documento enorme ou uma bomba de
        // compressão não chega a ser acumulado inteiro na memória. Metadados, como o título, não contam.
        WriteOutContentHandler plain = new WriteOutContentHandler(MAX_TEXT + 1);
        try {
            // Documentos embutidos (outro .docx, um zip, uma planilha) não são abertos: nada fora do texto do
            // próprio documento importa, e cada um seria uma nova chance de bomba de compressão.
            ParseContext context = new ParseContext();
            context.set(org.apache.tika.parser.Parser.class, org.apache.tika.parser.EmptyParser.INSTANCE);
            new AutoDetectParser().parse(in, new TeeContentHandler(xhtml, new BodyContentHandler(plain)), new Metadata(),
                    context);
        } catch (org.xml.sax.SAXException | org.apache.tika.exception.TikaException e) {
            throw new ImportFailure(WriteLimitReachedException.isWriteLimitReached(e) ? "text_too_long" : "import_failed", e);
        }
        // O XHTML que vai ao modelo também tem teto: metadados, como o título, entram nele em atributos de
        // <meta>, fora do texto do corpo, e o tamanho deles já vem limitado pela conferência das partes XML.
        String forModel = xhtml.toString();
        if (forModel.length() > MAX_FOR_MODEL) {
            throw new ImportFailure("text_too_long");
        }
        return new Extracted(forModel, plain.toString());
    }
}
