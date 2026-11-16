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


    private static final Pattern GOOGLE_DOC = Pattern.compile("^https://docs\\.google\\.com/document/d/([A-Za-z0-9_-]+)(/.*)?$");

    private final ExtractionProcess extraction;
    private final RestClient http;
    private final String googleDocsBaseUrl;

    DocumentText(ExtractionProcess extraction, RestClient.Builder builder,
            @Value("${app.import.google-docs-base-url}") String googleDocsBaseUrl) {
        this.extraction = extraction;
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
            Extracted extracted = extraction.extract(file, name.endsWith(".pdf") ? TextExtraction.PDF : TextExtraction.DOCX);
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
            return extraction.extract(copy, TextExtraction.DOCX);
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

}
