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
                { "GET", "/api//me", "400", "request_rejected" },
                { "GET", "/api/me;extra", "400", "request_rejected" },
                { "TRACE", "/api/me", "405", "method_not_allowed" },
                { "GET", "/api/a%2Fb", "400", "invalid_request" },
                { "GET", "/api/%00", "400", "invalid_request" },
                { "GET", "/api/%zz", "400", "invalid_request" },

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
        }

        // Linha de pedido recusada inteira: não há caminho guardado, e o instance é "/".
        Raw rawByte = send("GET", "/api/\u00e9");
        assertThat(rawByte.status()).isEqualTo(400);
        assertThat(rawByte.contentType()).startsWith("application/problem+json");
        var rawBody = json.readTree(rawByte.body());
        assertThat(rawBody.get("code").asString()).isEqualTo("invalid_request");
        assertThat(rawBody.get("instance").asString()).isEqualTo("/");
    }

    private Raw send(String method, String path) throws IOException {
        try (Socket socket = new Socket("localhost", port)) {
            OutputStream out = socket.getOutputStream();
            out.write((method + " " + path + " HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n")
                    .getBytes(StandardCharsets.ISO_8859_1));
            out.flush();
            String raw = new String(socket.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            String head = raw.substring(0, raw.indexOf("\r\n\r\n"));
            String body = raw.substring(raw.indexOf("\r\n\r\n") + 4);
            if (head.toLowerCase().contains("transfer-encoding: chunked")) {
                body = unchunk(body);
            }
            String contentType = head.lines().filter(l -> l.toLowerCase().startsWith("content-type:"))
                    .map(l -> l.substring(l.indexOf(':') + 1).strip()).findFirst().orElse("");
            return new Raw(Integer.parseInt(head.split(" ")[1]), contentType, body);
        }
    }

    private static String unchunk(String chunked) {
        StringBuilder body = new StringBuilder();
        int at = 0;
        while (true) {
            int lineEnd = chunked.indexOf("\r\n", at);
            int size = Integer.parseInt(chunked.substring(at, lineEnd).strip(), 16);
            if (size == 0) {
                return body.toString();
            }
            body.append(chunked, lineEnd + 2, lineEnd + 2 + size);
            at = lineEnd + 2 + size + 2;
        }
    }
}
