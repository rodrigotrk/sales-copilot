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
            try (InputStream in = Files.newInputStream(file)) {
                Extracted extracted = parse(in);
                if (name.endsWith(".pdf") && extracted.plain().isBlank()) {
                    throw new ImportFailure("pdf_without_text");
                }
                return extracted;
            }
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
            body = http.get().uri(export).retrieve().body(byte[].class);
        } catch (RuntimeException e) {
            throw new ImportFailure("google_docs_unavailable", e);
        }
        // Documento não compartilhado devolve a página de login do Google (HTML), não um .docx (zip "PK").
        if (body == null || body.length < 2 || body[0] != 'P' || body[1] != 'K') {
            throw new ImportFailure("google_docs_unavailable");
        }
        try {
            return parse(new java.io.ByteArrayInputStream(body));
        } catch (IOException e) {
            throw new ImportFailure("import_failed", e);
        }
    }

    private static Extracted parse(InputStream in) throws IOException, ImportFailure {
        ToXMLContentHandler xhtml = new ToXMLContentHandler();
        BodyContentHandler plain = new BodyContentHandler(-1);
        // Para de ler ao passar do limite de texto: um documento enorme ou uma bomba de compressão
        // não chega a ser acumulado inteiro na memória.
        WriteOutContentHandler limited = new WriteOutContentHandler(new TeeContentHandler(xhtml, plain), MAX_TEXT + 1);
        try {
            new AutoDetectParser().parse(in, limited, new Metadata(), new ParseContext());
        } catch (org.xml.sax.SAXException | org.apache.tika.exception.TikaException e) {
            throw new ImportFailure(WriteLimitReachedException.isWriteLimitReached(e) ? "text_too_long" : "import_failed", e);
        }
        return new Extracted(xhtml.toString(), plain.toString());
    }
}
