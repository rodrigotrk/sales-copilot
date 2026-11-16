package app.salescopilot.importing;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Ponto de entrada do processo isolado (AD-063): lê o documento, grava `model.txt` e `plain.txt`, ou
 * `error.txt` com o código da falha, na pasta indicada. Esgotar a memória encerra só este processo.
 * Argumentos: arquivo, tipo esperado, pasta de saída.
 */
public final class IsolatedExtraction {

    private IsolatedExtraction() {
    }

    public static void main(String[] args) throws Exception {
        Path file = Path.of(args[0]);
        Path out = Path.of(args[2]);
        try {
            DocumentText.Extracted extracted = TextExtraction.extract(file, args[1]);
            Files.writeString(out.resolve("model.txt"), extracted.forModel(), StandardCharsets.UTF_8);
            Files.writeString(out.resolve("plain.txt"), extracted.plain(), StandardCharsets.UTF_8);
        } catch (ImportFailure failure) {
            Files.writeString(out.resolve("error.txt"), failure.code(), StandardCharsets.UTF_8);
        }
    }
}
