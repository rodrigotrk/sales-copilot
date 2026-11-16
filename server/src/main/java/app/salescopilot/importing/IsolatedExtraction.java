package app.salescopilot.importing;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.FileDescriptor;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/**
 * Ponto de entrada do processo isolado (AD-063). O resultado sai pela saída padrão, nunca em arquivo: o
 * texto extraído não é gravado em disco (AC 87). Tudo o que bibliotecas imprimiriam na saída padrão vai para
 * a de erro. Argumentos: arquivo e tipo esperado.
 */
public final class IsolatedExtraction {

    static final String OK = "ok";
    static final String ERROR = "error";

    private IsolatedExtraction() {
    }

    public static void main(String[] args) throws Exception {
        var result = new DataOutputStream(new FileOutputStream(FileDescriptor.out));
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.err), true, StandardCharsets.UTF_8));
        try {
            DocumentText.Extracted extracted = TextExtraction.extract(Path.of(args[0]), args[1]);
            result.writeUTF(OK);
            write(result, extracted.forModel());
            write(result, extracted.plain());
        } catch (ImportFailure failure) {
            result.writeUTF(ERROR);
            result.writeUTF(failure.code());
        }
        result.flush();
    }

    private static void write(DataOutputStream out, String text) throws java.io.IOException {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        out.writeInt(bytes.length);
        out.write(bytes);
    }
}
