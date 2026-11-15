package app.salescopilot.shared;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import app.salescopilot.support.IntegrationTest;

/** Por HTTP real: o MockMvc não passa pelo Tomcat, e a recusa do firewall depende do endereço cru. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FirewallProblemsTest extends IntegrationTest {

    @LocalServerPort
    int port;

    @Test
    @DisplayName("[C103] endereço recusado pelo firewall sai em RFC 9457 com request_rejected")
    void firewallRejectionIsAProblem() throws Exception {
        var http = HttpClient.newHttpClient();
        for (String path : new String[] { "/api//me", "/api/me;extra" }) {
            HttpResponse<String> response = http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).build(),
                    HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).as(path).isEqualTo(400);
            assertThat(response.headers().firstValue("Content-Type")).as(path).hasValue("application/problem+json");
            var body = json.readTree(response.body());
            assertThat(body.get("code").asString()).as(path).isEqualTo("request_rejected");
            assertThat(body.get("type").asString()).as(path).isEqualTo("/problems/request_rejected");
        }
    }
}
