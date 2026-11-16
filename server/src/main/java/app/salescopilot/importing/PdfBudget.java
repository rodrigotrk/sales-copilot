package app.salescopilot.importing;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.zip.InflaterInputStream;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSObjectKey;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;

/**
 * Conferência do PDF antes do Tika: um PDF de 300 KB pode guardar centenas de MB comprimidos (nos
 * metadados XMP, por exemplo), que o parser descompacta inteiros na memória. Cada stream é descompactado
 * em pedaços, contando os bytes produzidos, e o arquivo é recusado acima do orçamento. Imagens ficam de
 * fora: o Tika não as lê como texto.
 */
final class PdfBudget {

    static final long MAX_STREAM_BYTES = 50L * 1024 * 1024;
    static final long MAX_TOTAL_BYTES = 200L * 1024 * 1024;

    private static final Set<COSName> IMAGE_FILTERS = Set.of(COSName.DCT_DECODE, COSName.JPX_DECODE, COSName.JBIG2_DECODE,
            COSName.CCITTFAX_DECODE);

    private PdfBudget() {
    }

    static void check(Path file) throws IOException, ImportFailure {
        try (PDDocument document = Loader.loadPDF(file.toFile(), IOUtils.createTempFileOnlyStreamCache())) {
            byte[] buffer = new byte[64 * 1024];
            long total = 0;
            for (COSObjectKey key : new ArrayList<>(document.getDocument().getXrefTable().keySet())) {
                COSBase object = document.getDocument().getObjectFromPool(key).getObject();
                if (!(object instanceof COSStream stream)) {
                    continue;
                }
                List<COSName> filters = filters(stream);
                if (filters.stream().anyMatch(IMAGE_FILTERS::contains)) {
                    continue;
                }
                long expanded = 0;
                try (InputStream in = decoded(stream, filters)) {
                    int read;
                    while ((read = in.read(buffer)) > 0) {
                        expanded += read;
                        total += read;
                        if (expanded > MAX_STREAM_BYTES || total > MAX_TOTAL_BYTES) {
                            throw new ImportFailure("text_too_long");
                        }
                    }
                }
            }
        }
    }

    private static List<COSName> filters(COSStream stream) {
        COSBase filter = stream.getDictionaryObject(COSName.FILTER);
        List<COSName> names = new ArrayList<>();
        if (filter instanceof COSName name) {
            names.add(name);
        } else if (filter instanceof COSArray array) {
            for (COSBase item : array) {
                if (item instanceof COSName name) {
                    names.add(name);
                }
            }
        }
        return names;
    }

    /**
     * FlateDecode, o filtro dos streams de texto e metadados, é descompactado em streaming. Os demais
     * filtros passam pelo PDFBox, que respeita o cache em disco.
     */
    private static InputStream decoded(COSStream stream, List<COSName> filters) throws IOException {
        if (filters.equals(List.of(COSName.FLATE_DECODE))) {
            return new InflaterInputStream(stream.createRawInputStream());
        }
        if (filters.isEmpty()) {
            return stream.createRawInputStream();
        }
        return stream.createInputStream();
    }
}
