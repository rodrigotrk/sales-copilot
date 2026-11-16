package app.salescopilot.importing;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.tika.exception.TikaException;
import org.apache.tika.exception.WriteLimitReachedException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.EmptyParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.apache.tika.sax.TeeContentHandler;
import org.apache.tika.sax.ToXMLContentHandler;
import org.apache.tika.sax.WriteOutContentHandler;
import org.xml.sax.SAXException;

/**
 * Texto de um .docx ou .pdf pelo Tika (AD-027). Roda dentro do processo isolado ({@link IsolatedExtraction}):
 * um documento malicioso que esgote a memória derruba só aquele processo (AD-063). O modelo recebe o corpo
 * em XHTML, que mantém as tabelas do .docx; a conferência de valores em dinheiro usa o texto puro.
 */
final class TextExtraction {

    static final String DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    static final String PDF = "application/pdf";
    private static final String OOXML = "application/x-tika-ooxml";

    static final int MAX_TEXT = ImportWorker.MAX_TEXT;
    /** Teto do XHTML enviado ao modelo: o texto do corpo com as tags das tabelas cabe com folga. */
    static final int MAX_FOR_MODEL = 2_000_000;

    private TextExtraction() {
    }

    /** O conteúdo precisa ser do tipo que a extensão promete: um zip enviado como .pdf não chega ao parser. */
    static DocumentText.Extracted extract(Path file, String expectedType) throws IOException, ImportFailure {
        String detected;
        try (TikaInputStream in = TikaInputStream.get(file)) {
            detected = new AutoDetectParser().getDetector().detect(in, new Metadata()).getBaseType().toString();
        }
        // O detector às vezes só reconhece o pacote OOXML genérico; o parser do .docx confere o resto.
        boolean matches = detected.equals(expectedType) || (expectedType.equals(DOCX) && detected.equals(OOXML));
        if (!matches) {
            throw new ImportFailure("import_failed");
        }
        ToXMLContentHandler xhtml = new ToXMLContentHandler();
        // Para de ler quando o texto do corpo passa do limite; metadados, como o título, não contam.
        WriteOutContentHandler plain = new WriteOutContentHandler(MAX_TEXT + 1);
        try (InputStream in = Files.newInputStream(file)) {
            // Documentos embutidos (outro .docx, um zip, uma planilha) não são abertos: nada fora do texto do
            // próprio documento importa.
            ParseContext context = new ParseContext();
            context.set(Parser.class, EmptyParser.INSTANCE);
            new AutoDetectParser().parse(in, new TeeContentHandler(xhtml, new BodyContentHandler(plain)), new Metadata(),
                    context);
        } catch (SAXException | TikaException e) {
            throw new ImportFailure(WriteLimitReachedException.isWriteLimitReached(e) ? "text_too_long" : "import_failed", e);
        }
        // Ao modelo vai só o corpo: metadados ficam no <head> e de fora do que ele lê. O corpo também tem teto,
        // porque as tags de tabelas multiplicam o tamanho do texto.
        String forModel = body(xhtml.toString());
        if (forModel.length() > MAX_FOR_MODEL) {
            throw new ImportFailure("text_too_long");
        }
        return new DocumentText.Extracted(forModel, plain.toString());
    }

    private static String body(String document) {
        int start = document.indexOf("<body");
        int end = document.lastIndexOf("</body>");
        // Sem corpo (o parser parou nos metadados), não há texto do documento para o modelo.
        return start < 0 || end < start ? "" : document.substring(start, end + "</body>".length());
    }
}
