package app.salescopilot.shared;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import app.salescopilot.support.IntegrationTest;

/**
 * Por HTTP real, com o pedido escrito à mão no socket: o MockMvc não passa pelo Tomcat, e clientes
 * HTTP recusam endereços como `%zz` antes de enviar.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FirewallProblemsTest extends IntegrationTest {

    @LocalServerPort
    int port;

    record Raw(int status, String contentType, String body) {
    }

    @Test
    @DisplayName("[C103] erro recusado antes do MVC sai em RFC 9457 com code e type")
    void rejectionsBeforeMvcAreProblems() throws Exception {
        String[][] cases = {
                { "GET", "/api//me", "400", "request_rejected", "Endereço de requisição inválido." },
                { "GET", "/api/me;extra", "400", "request_rejected", "Endereço de requisição inválido." },
                { "TRACE", "/api/me", "405", "method_not_allowed", "Método não permitido neste endereço." },
                { "GET", "/api/a%2Fb", "400", "invalid_request", "Requisição inválida." },
                { "GET", "/api/%00", "400", "invalid_request", "Requisição inválida." },
                { "GET", "/api/%zz", "400", "invalid_request", "Requisição inválida." },

        };
        for (String[] c : cases) {
            Raw response = send(c[0], c[1]);
            String label = c[0] + " " + c[1];
            assertThat(response.status()).as(label).isEqualTo(Integer.parseInt(c[2]));
            assertThat(response.contentType()).as(label).startsWith("application/problem+json");
            var body = json.readTree(response.body());
            assertThat(body.get("code").asString()).as(label).isEqualTo(c[3]);
            assertThat(body.get("type").asString()).as(label).isEqualTo("/problems/" + c[3]);
            assertThat(body.get("instance").asString()).as(label).isEqualTo(c[1]);
            assertThat(body.get("detail").asString()).as(label).isEqualTo(c[4]);
        }

        // Linha de pedido recusada inteira: não há caminho guardado, e o instance é "/".
        Raw rawByte = send("GET", "/api/\u00e9");
        assertThat(rawByte.status()).isEqualTo(400);
        assertThat(rawByte.contentType()).startsWith("application/problem+json");
        var rawBody = json.readTree(rawByte.body());
        assertThat(rawBody.get("code").asString()).isEqualTo("invalid_request");
        assertThat(rawBody.get("instance").asString()).isEqualTo("/");
        assertThat(rawBody.get("type").asString()).isEqualTo("/problems/invalid_request");
        assertThat(rawBody.get("detail").asString()).isEqualTo("Requisição inválida.");
    }

    private Raw send(String method, String path) throws IOException {
        try (Socket socket = new Socket("localhost", port)) {
            OutputStream out = socket.getOutputStream();
            out.write((method + " " + path + " HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n")
                    .getBytes(StandardCharsets.UTF_8));
            out.flush();
            byte[] raw = socket.getInputStream().readAllBytes();
            int headEnd = indexOf(raw, "\r\n\r\n".getBytes(StandardCharsets.ISO_8859_1), 0);
            String head = new String(raw, 0, headEnd, StandardCharsets.ISO_8859_1);
            byte[] body = java.util.Arrays.copyOfRange(raw, headEnd + 4, raw.length);
            if (head.toLowerCase().contains("transfer-encoding: chunked")) {
                body = unchunk(body);
            }
            String contentType = head.lines().filter(l -> l.toLowerCase().startsWith("content-type:"))
                    .map(l -> l.substring(l.indexOf(':') + 1).strip()).findFirst().orElse("");
            return new Raw(Integer.parseInt(head.split(" ")[1]), contentType, new String(body, StandardCharsets.UTF_8));
        }
    }

    /** O tamanho de cada pedaço vem em bytes, então o corpo é remontado em bytes antes de virar texto. */
    private static byte[] unchunk(byte[] chunked) {
        var body = new java.io.ByteArrayOutputStream();
        int at = 0;
        while (true) {
            int lineEnd = indexOf(chunked, "\r\n".getBytes(StandardCharsets.ISO_8859_1), at);
            int size = Integer.parseInt(new String(chunked, at, lineEnd - at, StandardCharsets.ISO_8859_1).strip(), 16);
            if (size == 0) {
                return body.toByteArray();
            }
            body.write(chunked, lineEnd + 2, size);
            at = lineEnd + 2 + size + 2;
        }
    }

    private static int indexOf(byte[] data, byte[] target, int from) {
        outer:
        for (int i = from; i <= data.length - target.length; i++) {
            for (int j = 0; j < target.length; j++) {
                if (data[i + j] != target[j]) {
                    continue outer;
                }
            }
            return i;
        }
        throw new IllegalStateException("resposta HTTP incompleta");
    }
}
