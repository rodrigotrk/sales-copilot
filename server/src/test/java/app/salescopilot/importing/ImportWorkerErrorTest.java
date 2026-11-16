package app.salescopilot.importing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import app.salescopilot.support.IntegrationTest;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** Um Error do parser, como o esgotamento de memória por um documento malicioso, também encerra a tarefa. */
@org.junit.jupiter.api.extension.ExtendWith(org.springframework.boot.test.system.OutputCaptureExtension.class)
class ImportWorkerErrorTest extends IntegrationTest {

    @MockitoSpyBean
    DocumentText documents;

    @Autowired
    ImportWorker worker;

    @Test
    @DisplayName("[C122] Error ao ler o documento encerra a tarefa como import_failed, apaga o arquivo e fica no log")
    void errorFinishesTheJob(org.springframework.boot.test.system.CapturedOutput output) throws Exception {
        doThrow(new OutOfMemoryError("documento malicioso")).when(documents).fromFile(any());
        Gestor g = newGestor();
        String playbookId = json.readTree(mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get("/api/businesses/" + g.businessId() + "/playbook").cookie(g.session()))
                .andReturn().getResponse().getContentAsString()).get("playbookId").asString();
        String body = mvc.perform(post("/api/businesses/" + g.businessId() + "/playbook/imports").cookie(g.session()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"playbookId\":\"" + playbookId + "\",\"text\":\"Etapas: Abertura.\"}"))
                .andReturn().getResponse().getContentAsString();
        UUID jobId = UUID.fromString(json.readTree(body).get("jobId").asString());
        String file = (String) jobRow(jobId).get("file_path");
        assertThat(java.nio.file.Files.exists(IMPORT_DIRECTORY.resolve(file))).isTrue();

        worker.drain();

        assertThat(jobRow(jobId)).containsEntry("status", "failed").containsEntry("error_code", "import_failed");
        assertThat(file).isNotNull();
        assertThat(java.nio.file.Files.exists(IMPORT_DIRECTORY.resolve(file))).isFalse();
        assertThat(output.getAll()).contains("Importação " + jobId + " falhou com erro inesperado")
                .contains("documento malicioso");
    }
}
