package app.salescopilot.importing;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipInputStream;

/**
 * Conferência do .docx antes do Tika: descompacta em streaming, descartando o conteúdo, e recusa o
 * arquivo se uma entrada ou o total passa do orçamento. O POI lê partes como os metadados inteiras na
 * memória, antes de qualquer limite de texto, e um .docx de 50 KB pode expandir para gigabytes.
 */
final class ZipBudget {

    /** Uma parte XML com mais de 20 MB já traz bem mais que os 100 mil caracteres aceitos (AC 77). */
    static final long MAX_ENTRY_BYTES = 20L * 1024 * 1024;
    static final long MAX_TOTAL_BYTES = 100L * 1024 * 1024;

    private ZipBudget() {
    }

    static void check(InputStream in) throws IOException, ImportFailure {
        try (ZipInputStream zip = new ZipInputStream(in)) {
            byte[] buffer = new byte[64 * 1024];
            long total = 0;
            while (zip.getNextEntry() != null) {
                long entry = 0;
                int read;
                while ((read = zip.read(buffer)) > 0) {
                    entry += read;
                    total += read;
                    if (entry > MAX_ENTRY_BYTES || total > MAX_TOTAL_BYTES) {
                        throw new ImportFailure("text_too_long");
                    }
                }
            }
        }
    }
}
