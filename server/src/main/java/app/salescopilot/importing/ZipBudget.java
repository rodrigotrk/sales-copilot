package app.salescopilot.importing;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Conferência do .docx antes do Tika: descompacta cada parte, descartando o conteúdo, e recusa o arquivo
 * se uma parte tem cara de bomba de compressão ou se o total passa do orçamento. O POI lê partes como os
 * metadados inteiras na memória, antes de qualquer limite de texto, e um .docx de 50 KB pode expandir
 * para gigabytes. As partes vêm do diretório central, como o parser as lê: uma assinatura falsa no meio
 * do arquivo não esconde nenhuma.
 */
final class ZipBudget {

    /** Acima disto, uma parte só passa se comprimir menos de 100 para 1, como uma imagem de verdade. */
    static final long LARGE_ENTRY_BYTES = 20L * 1024 * 1024;
    static final long MAX_RATIO = 100;
    static final long MAX_TOTAL_BYTES = 100L * 1024 * 1024;

    private ZipBudget() {
    }

    static void check(Path file) throws IOException, ImportFailure {
        try (ZipFile zip = new ZipFile(file.toFile())) {
            byte[] buffer = new byte[64 * 1024];
            long total = 0;
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                long compressed = Math.max(entry.getCompressedSize(), 1);
                long expanded = 0;
                try (InputStream in = zip.getInputStream(entry)) {
                    int read;
                    while ((read = in.read(buffer)) > 0) {
                        expanded += read;
                        total += read;
                        boolean bomb = expanded > LARGE_ENTRY_BYTES && expanded > compressed * MAX_RATIO;
                        if (bomb || total > MAX_TOTAL_BYTES) {
                            throw new ImportFailure("text_too_long");
                        }
                    }
                }
            }
        }
    }
}
