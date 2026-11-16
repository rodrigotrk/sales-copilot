package app.salescopilot.importing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** O protocolo entre o processo de extração e o servidor só aceita um resultado completo e bem formado. */
class ExtractionProtocolTest {

    @Test
    @DisplayName("[C130] resultado do filho com status desconhecido ou texto cortado vira import_failed")
    void onlyCompleteResultsAreAccepted() throws Exception {
        assertThat(ExtractionProcess.decode(result("ok", "<body>Abertura</body>", "Abertura", 0)).plain()).isEqualTo("Abertura");
        assertThatThrownBy(() -> ExtractionProcess.decode(result("talvez", "<body/>", "x", 0)))
                .isInstanceOfSatisfying(ImportFailure.class, f -> assertThat(f.code()).isEqualTo("import_failed"));
        assertThatThrownBy(() -> ExtractionProcess.decode(result("ok", "<body>Abertura</body>", "Abertura", 5)))
                .isInstanceOfSatisfying(ImportFailure.class, f -> assertThat(f.code()).isEqualTo("import_failed"));
    }

    /** `cut` bytes a menos no fim: um filho que caiu no meio da escrita. */
    private static byte[] result(String status, String model, String plain, int cut) throws Exception {
        var bytes = new ByteArrayOutputStream();
        var out = new DataOutputStream(bytes);
        out.writeUTF(status);
        for (String text : new String[] { model, plain }) {
            byte[] encoded = text.getBytes(StandardCharsets.UTF_8);
            out.writeInt(encoded.length);
            out.write(encoded);
        }
        byte[] all = bytes.toByteArray();
        return java.util.Arrays.copyOf(all, all.length - cut);
    }
}
