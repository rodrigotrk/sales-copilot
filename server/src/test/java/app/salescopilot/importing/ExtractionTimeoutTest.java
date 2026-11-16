package app.salescopilot.importing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;

import app.salescopilot.support.IntegrationTest;

/** Um processo de extração que passa do tempo limite é encerrado, e a tarefa termina em import_failed. */
@ExtendWith(OutputCaptureExtension.class)
@TestPropertySource(properties = "app.import.extraction-timeout=1ms")
class ExtractionTimeoutTest extends IntegrationTest {

    @Autowired
    ImportWorker worker;

    @Test
    @DisplayName("[C126] extração que passa do tempo limite é encerrada e a tarefa termina em import_failed")
    void extractionTimeout(CapturedOutput output) throws Exception {
        Gestor g = newGestor();
        String playbookId = json.readTree(mvc.perform(get("/api/businesses/" + g.businessId() + "/playbook").cookie(g.session()))
                .andReturn().getResponse().getContentAsString()).get("playbookId").asString();
        byte[] docx;
        try (var document = new org.apache.poi.xwpf.usermodel.XWPFDocument(); var out = new java.io.ByteArrayOutputStream()) {
            document.createParagraph().createRun().setText("Abertura");
            document.write(out);
            docx = out.toByteArray();
        }
        String body = mvc.perform(multipart("/api/businesses/" + g.businessId() + "/playbook/imports")
                .file(new MockMultipartFile("file", "lento.docx",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document", docx))
                .param("playbookId", playbookId).cookie(g.session()).with(csrf()))
                .andReturn().getResponse().getContentAsString();
        UUID jobId = UUID.fromString(json.readTree(body).get("jobId").asString());

        worker.drain();

        assertThat(jobRow(jobId)).containsEntry("status", "failed").containsEntry("error_code", "import_failed");
        assertThat(output.getAll()).contains("passou de PT0.001S e foi encerrada");
    }

    @Autowired
    ExtractionProcess extraction;

    @Test
    @DisplayName("[C127] extração interrompida encerra o processo filho")
    void interruptedExtractionKillsTheChild() throws Exception {
        java.nio.file.Path file = java.nio.file.Files.createTempFile("interrompida", ".docx");
        // Um título de 600 MB num filho com 4 GB: a extração leva vários segundos e é interrompida no meio.
        byte[] base;
        try (var document = new org.apache.poi.xwpf.usermodel.XWPFDocument(); var out = new java.io.ByteArrayOutputStream()) {
            document.createParagraph().createRun().setText("Abertura");
            document.write(out);
            base = out.toByteArray();
        }
        try (var in = new java.util.zip.ZipInputStream(new java.io.ByteArrayInputStream(base));
                var zip = new java.util.zip.ZipOutputStream(java.nio.file.Files.newOutputStream(file))) {
            java.util.zip.ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                if (entry.getName().equals("docProps/core.xml")) {
                    continue;
                }
                zip.putNextEntry(new java.util.zip.ZipEntry(entry.getName()));
                in.transferTo(zip);
                zip.closeEntry();
            }
            zip.putNextEntry(new java.util.zip.ZipEntry("docProps/core.xml"));
            zip.write(("<cp:coreProperties xmlns:cp=\"http://schemas.openxmlformats.org/package/2006/metadata/core-properties\" "
                    + "xmlns:dc=\"http://purl.org/dc/elements/1.1/\"><dc:title>").getBytes(java.nio.charset.StandardCharsets.UTF_8));
            byte[] chunk = "a".repeat(1024 * 1024).getBytes(java.nio.charset.StandardCharsets.UTF_8);
            for (int i = 0; i < 600; i++) {
                zip.write(chunk);
            }
            zip.write("</dc:title></cp:coreProperties>".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        var slow = new ExtractionProcess("4g", java.time.Duration.ofMinutes(5));
        Thread worker = new Thread(() -> {
            try {
                slow.extract(file, TextExtraction.DOCX);
            } catch (ImportFailure expected) {
                // A interrupção termina a extração como falha.
            }
        });
        worker.start();
        long deadline = System.currentTimeMillis() + 10_000;
        while (ProcessHandle.current().children().noneMatch(ProcessHandle::isAlive)) {
            assertThat(System.currentTimeMillis()).as("processo filho iniciado").isLessThan(deadline);
            Thread.sleep(10);
        }
        Thread.sleep(1_000);
        var children = ProcessHandle.current().children().filter(ProcessHandle::isAlive).toList();
        assertThat(children).as("filho ainda extraindo na hora da interrupção").isNotEmpty();
        worker.interrupt();
        worker.join(10_000);
        for (ProcessHandle child : children) {
            // Sem encerrar o filho, ele seguiria extraindo por mais alguns segundos.
            child.onExit().get(500, java.util.concurrent.TimeUnit.MILLISECONDS);
            assertThat(child.isAlive()).isFalse();
        }
        java.nio.file.Files.deleteIfExists(file);
    }
}
