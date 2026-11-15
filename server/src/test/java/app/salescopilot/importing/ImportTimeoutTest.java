package app.salescopilot.importing;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

import app.salescopilot.support.IntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * O tempo limite real é de 120 s (application.yml, conferido no C81); aqui ele cai para 1 s para
 * provar que uma resposta atrasada conta como tentativa falha.
 */
@TestPropertySource(properties = "spring.ai.openai.timeout=1s")
class ImportTimeoutTest extends IntegrationTest {

    @Autowired
    ImportWorker worker;

    @Test
    @DisplayName("[C81b] resposta do modelo depois do tempo limite conta como falha e repete até 3 vezes")
    void slowModelCountsAsFailure() throws Exception {
        admin.sql("UPDATE import_jobs SET status = 'failed' WHERE status IN ('queued', 'running')").update();
        Gestor g = newGestor();
        UUID playbookId = UUID.fromString(getPlaybook(g).get("playbookId").asString());
        STUB.modelDelay(Duration.ofSeconds(2));
        String body = mvc.perform(post("/api/businesses/" + g.businessId() + "/playbook/imports").cookie(g.session()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("playbookId", playbookId.toString(), "text", "Etapa: Abertura"))))
                .andReturn().getResponse().getContentAsString();
        UUID jobId = UUID.fromString(json.readTree(body).get("jobId").asString());

        worker.drain();

        assertThat(jobRow(jobId)).containsEntry("status", "failed").containsEntry("error_code", "model_failed");
        assertThat(STUB.modelRequests()).hasSize(3);
    }
}
