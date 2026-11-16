package app.salescopilot.importing;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.support.TransactionTemplate;

import app.salescopilot.importing.domain.ExtractedPlaybook;
import app.salescopilot.playbook.api.PlaybookDrafts;
import app.salescopilot.shared.Tenancy;
import app.salescopilot.support.Drafts;
import app.salescopilot.support.IntegrationTest;
import app.salescopilot.support.StubServer;
import app.salescopilot.support.TestDatabase;
import tools.jackson.databind.JsonNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@org.junit.jupiter.api.extension.ExtendWith(org.springframework.boot.test.system.OutputCaptureExtension.class)
class ImportTest extends IntegrationTest {

    private static final String DOCUMENT = """
            Etapas: Abertura e Diagnóstico.
            Descobrir a conta de luz: "Quanto você paga de luz por mês?"
            Objeção preço: "Está caro". Resposta: "O que você compararia com esse valor?"
            Residencial: 8 placas, R$ 4.500 à vista ou 6x de R$ 800.
            Desconto máximo na call: R$ 300.
            """;

    private static final String ANSWER = """
            {"stages":[{"name":"Abertura"},{"name":"Diagnóstico"}],
             "discovery":[{"information":"Conta de luz","question":"Quanto você paga de luz por mês?"}],
             "objections":[{"name":"Preço","leadSays":"Está caro","approvedResponse":"O que você compararia com esse valor?"}],
             "priceTable":{"extraColumnName":"Placas","maxDiscount":300,"maxDiscountExcerpt":"Desconto máximo na call: R$ 300.",
               "plans":[{"name":"Residencial","extraValue":"8","cash":4500,"installmentsCount":6,"installmentsValue":800,
                         "excerpt":"Residencial: 8 placas, R$ 4.500 à vista ou 6x de R$ 800."}]}}""";

    @Autowired
    ImportWorker worker;

    @Autowired
    PlaybookExtraction extraction;

    @Autowired
    ImportJobs jobs;

    @Autowired
    ImportFileSweeper sweeper;

    @Autowired
    PlaybookDrafts drafts;

    @Autowired
    Tenancy tenancy;

    @Autowired
    org.springframework.core.env.Environment environment;

    /** Tarefas deixadas por outras classes de teste não podem consumir as respostas programadas do modelo. */
    @BeforeEach
    void closeOtherJobs() {
        admin.sql("UPDATE import_jobs SET status = 'failed' WHERE status IN ('queued', 'running')").update();
    }

    @Test
    @DisplayName("[C66a] enviar a origem responde 202 e cria a tarefa queued")
    void enqueueCreatesQueuedJob() throws Exception {
        Gestor g = newGestor();
        String body = importText(g, DOCUMENT).andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString();
        UUID jobId = UUID.fromString(json.readTree(body).get("jobId").asString());

        var row = jobRow(jobId);
        assertThat(row.get("status")).isEqualTo("queued");
        assertThat(row.get("source_kind")).isEqualTo("text");
        assertThat(Files.exists(IMPORT_DIRECTORY.resolve((String) row.get("file_path")))).isTrue();
    }

    @Test
    @DisplayName("[C67a] GET do playbook devolve a tarefa ativa")
    void playbookShowsActiveJob() throws Exception {
        Gestor g = newGestor();
        importText(g, DOCUMENT);
        JsonNode job = getPlaybook(g).get("importJob");
        assertThat(job.get("status").asString()).isEqualTo("queued");
    }

    @Test
    @DisplayName("[C68a] importação acrescenta ao fim sem mudar o existente e conta os itens")
    void importAppendsWithoutChangingExistingItems() throws Exception {
        Gestor g = newGestor();
        UUID playbookId = playbookId(g);
        save(g, playbookId, Drafts.complete(), 0);
        JsonNode before = getPlaybook(g).get("draft");
        STUB.modelAnswers(ANSWER);
        UUID jobId = jobId(importText(g, DOCUMENT));

        worker.drain();

        JsonNode after = getPlaybook(g).get("draft");
        assertThat(after.get("stages").size()).isEqualTo(4);
        assertThat(after.get("stages").get(0)).isEqualTo(before.get("stages").get(0));
        assertThat(after.get("stages").get(1)).isEqualTo(before.get("stages").get(1));
        assertThat(after.get("stages").get(2).get("name").asString()).isEqualTo("Abertura");
        assertThat(after.get("priceTable").get("plans").size()).isEqualTo(3);
        assertThat(after.get("priceTable").get("plans").get(0)).isEqualTo(before.get("priceTable").get("plans").get(0));
        JsonNode job = getPlaybook(g).get("importJob");
        assertThat(job.get("status").asString()).isEqualTo("done");
        JsonNode counts = job.get("counts");
        assertThat(counts.get("stages").asInt()).isEqualTo(2);
        assertThat(counts.get("discovery").asInt()).isEqualTo(1);
        assertThat(counts.get("objections").asInt()).isEqualTo(1);
        assertThat(counts.get("plans").asInt()).isEqualTo(1);
        assertThat(jobRow(jobId).get("status")).isEqualTo("done");
    }

    @Test
    @DisplayName("[C69a] plano e desconto importados chegam marcados com o trecho e o resto sem marca")
    void importedPricesAreMarked() throws Exception {
        Gestor g = newGestor();
        STUB.modelAnswers(ANSWER);
        importText(g, DOCUMENT);

        worker.drain();

        JsonNode draft = getPlaybook(g).get("draft");
        JsonNode plan = draft.get("priceTable").get("plans").get(0);
        assertThat(plan.get("cash").asString()).isEqualTo("4500.00");
        assertThat(plan.get("review").get("status").asString()).isEqualTo("pending");
        assertThat(plan.get("review").get("excerpt").asString()).isEqualTo("Residencial: 8 placas, R$ 4.500 à vista ou 6x de R$ 800.");
        assertThat(draft.get("priceTable").get("maxDiscount").asString()).isEqualTo("300.00");
        assertThat(draft.get("priceTable").get("maxDiscountReview").get("excerpt").asString()).isEqualTo("Desconto máximo na call: R$ 300.");
        assertThat(draft.get("stages").get(0).has("review")).isFalse();
        assertThat(draft.get("objections").get(0).has("review")).isFalse();
    }

    @Test
    @DisplayName("[C74] extensão não aceita responde 415 sem criar tarefa")
    void unsupportedExtension() throws Exception {
        Gestor g = newGestor();
        importFile(g, new MockMultipartFile("file", "playbook.exe", "application/octet-stream", new byte[] {1, 2}))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(header().string("Content-Type", "application/problem+json"))
                .andExpect(jsonPath("$.code").value("unsupported_format"));
        assertThat(jobsOf(g.businessId())).isEmpty();
    }

    @Test
    @DisplayName("[C75] arquivo acima de 10 MB responde 413 sem criar tarefa")
    void fileTooLarge() throws Exception {
        Gestor g = newGestor();
        byte[] big = new byte[10 * 1024 * 1024 + 1];
        importFile(g, new MockMultipartFile("file", "playbook.txt", "text/plain", big))
                .andExpect(status().isContentTooLarge())
                .andExpect(jsonPath("$.code").value("file_too_large"));
        importFile(g, new MockMultipartFile("file", "playbook.txt", "text/plain", new byte[10 * 1024 * 1024]))
                .andExpect(status().isAccepted());
        assertThat(jobsOf(g.businessId())).hasSize(1);
    }

    @Test
    @DisplayName("[C76] segunda importação com tarefa ativa responde 409")
    void secondImportWhileActive() throws Exception {
        Gestor g = newGestor();
        importText(g, DOCUMENT).andExpect(status().isAccepted());
        importText(g, DOCUMENT).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("import_running"));
        assertThat(jobsOf(g.businessId())).hasSize(1);
    }

    @Test
    @DisplayName("[C77] texto acima de 100 mil caracteres falha sem chamar o modelo")
    void textTooLong() throws Exception {
        Gestor g = newGestor();
        UUID jobId = jobId(importText(g, "a".repeat(100_001)));
        worker.drain();
        assertThat(jobRow(jobId)).containsEntry("status", "failed").containsEntry("error_code", "text_too_long");
        assertThat(STUB.modelRequests()).isEmpty();

        UUID longDocx = jobId(importFile(g, new MockMultipartFile("file", "longo.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", docx(100_001))));
        worker.drain();
        assertThat(jobRow(longDocx)).containsEntry("error_code", "text_too_long");
        assertThat(STUB.modelRequests()).isEmpty();

        STUB.modelAnswers(ANSWER);
        UUID docxUnderLimit = jobId(importFile(g, new MockMultipartFile("file", "quase.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", docx(97_000))));
        worker.drain();
        assertThat(jobRow(docxUnderLimit)).containsEntry("status", "done");
        assertThat(STUB.modelRequests()).hasSize(1);

        STUB.modelAnswers(ANSWER);
        UUID longTitle = jobId(importFile(g, new MockMultipartFile("file", "titulo.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", docxWithTitle("t".repeat(100_001)))));
        worker.drain();
        assertThat(jobRow(longTitle)).containsEntry("status", "done");
        // O título fica nos metadados e não vai ao modelo, só o corpo.
        assertThat(STUB.modelRequests().getLast()).doesNotContain("t".repeat(1000));
        assertThat(STUB.modelRequests()).hasSize(2);

        UUID limit = jobId(importText(g, "a".repeat(100_000)));
        STUB.modelAnswers("{\"stages\":[],\"discovery\":[],\"objections\":[],\"priceTable\":{\"extraColumnName\":null,"
                + "\"maxDiscount\":null,\"maxDiscountExcerpt\":null,\"plans\":[]}}");
        worker.drain();
        assertThat(jobRow(limit)).containsEntry("error_code", "nothing_found");
    }

    @Test
    @DisplayName("[C78] PDF sem texto falha sem chamar o modelo")
    void pdfWithoutText() throws Exception {
        Gestor g = newGestor();
        var pdf = new ByteArrayOutputStream();
        try (var document = new PDDocument()) {
            document.addPage(new PDPage());
            document.save(pdf);
        }
        UUID jobId = jobId(importFile(g, new MockMultipartFile("file", "escaneado.pdf", "application/pdf", pdf.toByteArray())));
        worker.drain();
        assertThat(jobRow(jobId)).containsEntry("status", "failed").containsEntry("error_code", "pdf_without_text");
        assertThat(STUB.modelRequests()).isEmpty();
    }

    @Test
    @DisplayName("[C79] link que não é Google Docs ou não público falha sem mudar o rascunho")
    void googleDocsUnavailable() throws Exception {
        Gestor g = newGestor();
        long revision = revision(g);
        UUID notGoogle = jobId(importUrl(g, "https://exemplo.test/document/d/abc/edit"));
        worker.drain();
        assertThat(jobRow(notGoogle)).containsEntry("error_code", "google_docs_unavailable");

        STUB.document("privado", new StubServer.Reply(200, "text/html", "<html>Fazer login</html>".getBytes()));
        UUID privateDoc = jobId(importUrl(g, "https://docs.google.com/document/d/privado/edit"));
        worker.drain();
        assertThat(jobRow(privateDoc)).containsEntry("error_code", "google_docs_unavailable");
        assertThat(revision(g)).isEqualTo(revision);
        assertThat(STUB.modelRequests()).isEmpty();
    }

    @Test
    @DisplayName("[C116] exportação do Google Docs acima de 10 MB falha com text_too_long sem chamar o modelo")
    void googleDocsTooLarge() throws Exception {
        Gestor g = newGestor();
        long revision = revision(g);
        byte[] huge = new byte[Math.toIntExact(ImportController.MAX_FILE_BYTES) + 1];
        huge[0] = 'P';
        huge[1] = 'K';
        STUB.document("enorme", new StubServer.Reply(200, "application/vnd.openxmlformats-officedocument.wordprocessingml.document", huge));
        UUID jobId = jobId(importUrl(g, "https://docs.google.com/document/d/enorme/edit"));
        worker.drain();
        assertThat(jobRow(jobId)).containsEntry("status", "failed").containsEntry("error_code", "text_too_long");
        assertThat(revision(g)).isEqualTo(revision);
        assertThat(STUB.modelRequests()).isEmpty();

        // Uma exportação de 200 MB não é baixada inteira: a leitura para logo depois de 10 MB.
        STUB.documentStream("gigante", 200L * 1024 * 1024);
        UUID streamed = jobId(importUrl(g, "https://docs.google.com/document/d/gigante/edit"));
        worker.drain();
        assertThat(jobRow(streamed)).containsEntry("error_code", "text_too_long");
        assertThat(STUB.streamedBytes()).isLessThan(64L * 1024 * 1024);
    }

    @Test
    @DisplayName("[C121] documento malicioso esgota só o processo de extração: a tarefa falha sem chamar o modelo e o servidor segue")
    void maliciousDocumentOnlyKillsTheExtraction(org.springframework.boot.test.system.CapturedOutput output) throws Exception {
        Gestor g = newGestor();
        long revision = revision(g);
        // Um título de 600 MB comprimido em menos de 1 MB: o processo de extração, com 512 MB, esgota a memória.
        byte[] bomb = docxWithHugeTitle(600L * 1024 * 1024);
        assertThat(bomb.length).isLessThan(10 * 1024 * 1024);
        UUID jobId = jobId(importFile(g, new MockMultipartFile("file", "bomba.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", bomb)));
        String file = (String) jobRow(jobId).get("file_path");
        worker.drain();
        assertThat(jobRow(jobId)).containsEntry("status", "failed").containsEntry("error_code", "import_failed");
        assertThat(revision(g)).isEqualTo(revision);
        assertThat(STUB.modelRequests()).isEmpty();
        assertThat(Files.exists(IMPORT_DIRECTORY.resolve(file))).isFalse();
        // Quem esgotou a memória foi o processo de extração, não o servidor.
        assertThat(output.getAll()).contains("terminou com código").doesNotContain("falhou com erro inesperado");

        // O servidor segue: a importação seguinte, de um documento normal, conclui.
        STUB.modelAnswers(ANSWER);
        UUID normal = jobId(importText(g, DOCUMENT));
        worker.drain();
        assertThat(jobRow(normal)).containsEntry("status", "done");
        STUB.reset();

        // Títulos de 2 MB e de 19 MB: o parser para nos metadados, sem corpo, e nada chega ao modelo.
        for (long titleBytes : new long[] { 2L * 1024 * 1024, 19L * 1024 * 1024 }) {
            UUID titled = jobId(importFile(g, new MockMultipartFile("file", "titulo.docx",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document", docxWithHugeTitle(titleBytes))));
            worker.drain();
            assertThat(jobRow(titled)).containsEntry("status", "failed").containsEntry("error_code", "nothing_found");
            assertThat(STUB.modelRequests()).isEmpty();
        }

        // Uma imagem e um objeto OLE de 25 MB, de documentos legítimos, importam normalmente.
        for (String[] part : new String[][] {
                { "word/media/image1.bmp", "<Default Extension=\"bmp\" ContentType=\"image/bmp\"/>" },
                { "word/embeddings/oleObject1.bin",
                        "<Default Extension=\"bin\" ContentType=\"application/vnd.openxmlformats-officedocument.oleObject\"/>" } }) {
            STUB.modelAnswers(ANSWER);
            UUID legit = jobId(importFile(g, new MockMultipartFile("file", "legitimo.docx",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    docxWithPart(part[0], 25L * 1024 * 1024, part[1]))));
            worker.drain();
            assertThat(jobRow(legit)).as(part[0]).containsEntry("status", "done");
        }
    }

    @Test
    @DisplayName("[C124] conteúdo de outro tipo que a extensão falha e documento embutido não é aberto")
    void contentMustMatchExtension() throws Exception {
        Gestor g = newGestor();
        var zip = new ByteArrayOutputStream();
        try (var out = new java.util.zip.ZipOutputStream(zip)) {
            out.putNextEntry(new java.util.zip.ZipEntry("dentro.docx"));
            out.write(docx(10));
            out.closeEntry();
        }
        UUID jobId = jobId(importFile(g, new MockMultipartFile("file", "falso.pdf", "application/pdf", zip.toByteArray())));
        worker.drain();
        assertThat(jobRow(jobId)).containsEntry("status", "failed").containsEntry("error_code", "import_failed");
        assertThat(STUB.modelRequests()).isEmpty();

        // Um .docx embutido em outro não é aberto: o texto dele não chega ao modelo.
        byte[] inner;
        try (var document = new org.apache.poi.xwpf.usermodel.XWPFDocument(); var bytes = new ByteArrayOutputStream()) {
            document.createParagraph().createRun().setText("MARCADOR-EMBUTIDO");
            document.write(bytes);
            inner = bytes.toByteArray();
        }
        STUB.modelAnswers(ANSWER);
        UUID embedded = jobId(importFile(g, new MockMultipartFile("file", "embutido.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", docxWithEmbeddedDocx(inner))));
        worker.drain();
        assertThat(jobRow(embedded)).containsEntry("status", "done");
        assertThat(STUB.modelRequests()).hasSize(1);
        assertThat(STUB.modelRequests().getFirst()).doesNotContain("MARCADOR-EMBUTIDO");
    }

    /** Um .docx válido com a parte `name` de `bytes` que comprimem cerca de 5 para 1, e `typeDeclaration` em [Content_Types].xml. */
    private static byte[] docxWithPart(String name, long bytes, String typeDeclaration) throws java.io.IOException {
        byte[] base = rewriteDocx(docx(10), zip -> {
            zip.putNextEntry(new java.util.zip.ZipEntry(name));
            var colors = new java.util.Random(11);
            byte[] chunk = new byte[1024 * 1024];
            for (long written = 0; written < bytes; written += chunk.length) {
                for (int i = 0; i < chunk.length; i += 4) {
                    java.util.Arrays.fill(chunk, i, Math.min(i + 4, chunk.length), (byte) colors.nextInt(16));
                }
                zip.write(chunk);
            }
            zip.closeEntry();
        });
        return replaceEntry(base, "[Content_Types].xml", types -> types.replace("</Types>", typeDeclaration + "</Types>"));
    }

    /** Um .docx com outro .docx embutido como pacote, ligado ao documento por relacionamento. */
    private static byte[] docxWithEmbeddedDocx(byte[] inner) throws java.io.IOException {
        byte[] base = rewriteDocx(docx(10), zip -> {
            zip.putNextEntry(new java.util.zip.ZipEntry("word/embeddings/interno.docx"));
            zip.write(inner);
            zip.closeEntry();
        });
        base = replaceEntry(base, "[Content_Types].xml", types -> types.replace("</Types>",
                "<Default Extension=\"docx\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.document\"/></Types>"));
        return replaceEntry(base, "word/_rels/document.xml.rels", rels -> rels.replace("</Relationships>",
                "<Relationship Id=\"rIdEmbutido\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/package\" "
                        + "Target=\"embeddings/interno.docx\"/></Relationships>"));
    }

    private static byte[] replaceEntry(byte[] zipBytes, String name, java.util.function.UnaryOperator<String> change)
            throws java.io.IOException {
        var out = new ByteArrayOutputStream();
        boolean found = false;
        try (var in = new java.util.zip.ZipInputStream(new java.io.ByteArrayInputStream(zipBytes));
                var zip = new java.util.zip.ZipOutputStream(out)) {
            java.util.zip.ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                zip.putNextEntry(new java.util.zip.ZipEntry(entry.getName()));
                if (entry.getName().equals(name)) {
                    found = true;
                    zip.write(change.apply(new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8))
                            .getBytes(java.nio.charset.StandardCharsets.UTF_8));
                } else {
                    in.transferTo(zip);
                }
                zip.closeEntry();
            }
        }
        if (!found) {
            throw new IllegalStateException("parte ausente: " + name);
        }
        return out.toByteArray();
    }

    private interface ZipWriter {
        void write(java.util.zip.ZipOutputStream zip) throws java.io.IOException;
    }

    private static byte[] rewriteDocx(byte[] base, ZipWriter extra) throws java.io.IOException {
        var out = new ByteArrayOutputStream();
        try (var in = new java.util.zip.ZipInputStream(new java.io.ByteArrayInputStream(base));
                var zip = new java.util.zip.ZipOutputStream(out)) {
            java.util.zip.ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                zip.putNextEntry(new java.util.zip.ZipEntry(entry.getName()));
                if (entry.getName().equals("[Content_Types].xml")) {
                    // Declara o tipo das imagens acrescentadas, como um .docx de verdade.
                    String types = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                    zip.write(types.replace("<Default ", "<Default Extension=\"bmp\" ContentType=\"image/bmp\"/><Default ")
                            .getBytes(java.nio.charset.StandardCharsets.UTF_8));
                } else {
                    in.transferTo(zip);
                }
                zip.closeEntry();
            }
            extra.write(zip);
        }
        return out.toByteArray();
    }

    /** Um .docx válido em que docProps/core.xml traz um título de `titleBytes`, escrito em pedaços. */
    private static byte[] docxWithHugeTitle(long titleBytes) throws java.io.IOException {
        byte[] base = docx(10);
        var out = new ByteArrayOutputStream();
        try (var in = new java.util.zip.ZipInputStream(new java.io.ByteArrayInputStream(base));
                var zip = new java.util.zip.ZipOutputStream(out)) {
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
            zip.write(("<?xml version=\"1.0\" encoding=\"UTF-8\"?><cp:coreProperties "
                    + "xmlns:cp=\"http://schemas.openxmlformats.org/package/2006/metadata/core-properties\" "
                    + "xmlns:dc=\"http://purl.org/dc/elements/1.1/\"><dc:title>").getBytes(java.nio.charset.StandardCharsets.UTF_8));
            byte[] chunk = "a".repeat(1024 * 1024).getBytes(java.nio.charset.StandardCharsets.UTF_8);
            for (long written = 0; written < titleBytes; written += chunk.length) {
                zip.write(chunk);
            }
            zip.write("</dc:title></cp:coreProperties>".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return out.toByteArray();
    }

    @Test
    @DisplayName("[C125] PDF com metadados que descompactam para centenas de MB falha com import_failed sem chamar o modelo")
    void pdfBomb(org.springframework.boot.test.system.CapturedOutput output) throws Exception {
        Gestor g = newGestor();
        byte[] pdf;
        try (var document = new org.apache.pdfbox.pdmodel.PDDocument(); var out = new ByteArrayOutputStream()) {
            document.addPage(new org.apache.pdfbox.pdmodel.PDPage());
            var metadata = new org.apache.pdfbox.pdmodel.common.PDMetadata(document);
            try (var stream = metadata.createOutputStream(org.apache.pdfbox.cos.COSName.FLATE_DECODE)) {
                stream.write("<x:xmpmeta xmlns:x='adobe:ns:meta/'>".getBytes(java.nio.charset.StandardCharsets.UTF_8));
                byte[] chunk = "a".repeat(1024 * 1024).getBytes(java.nio.charset.StandardCharsets.UTF_8);
                for (int i = 0; i < 300; i++) {
                    stream.write(chunk);
                }
                stream.write("</x:xmpmeta>".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
            document.getDocumentCatalog().setMetadata(metadata);
            document.save(out);
            pdf = out.toByteArray();
        }
        assertThat(pdf.length).isLessThan(1024 * 1024);
        UUID jobId = jobId(importFile(g, new MockMultipartFile("file", "bomba.pdf", "application/pdf", pdf)));
        worker.drain();
        assertThat(jobRow(jobId)).containsEntry("status", "failed").containsEntry("error_code", "import_failed");
        assertThat(STUB.modelRequests()).isEmpty();
        assertThat(output.getAll()).contains("terminou com código").doesNotContain("falhou com erro inesperado");
    }

    @Test
    @DisplayName("[C80] documento sem itens falha sem mudar o rascunho")
    void nothingFound() throws Exception {
        Gestor g = newGestor();
        long revision = revision(g);
        STUB.modelAnswers("{\"stages\":[],\"discovery\":[],\"objections\":[],\"priceTable\":{\"extraColumnName\":null,"
                + "\"maxDiscount\":null,\"maxDiscountExcerpt\":null,\"plans\":[]}}");
        UUID jobId = jobId(importText(g, "Um texto sem playbook."));
        worker.drain();
        assertThat(jobRow(jobId)).containsEntry("status", "failed").containsEntry("error_code", "nothing_found");
        assertThat(revision(g)).isEqualTo(revision);
    }

    @Test
    @DisplayName("[C81] três falhas do modelo encerram a tarefa em failed sem mudar o rascunho")
    void modelFailsThreeTimes() throws Exception {
        Gestor g = newGestor();
        long revision = revision(g);
        STUB.modelFails(500);
        STUB.modelAnswers("isto não é JSON");
        STUB.modelFails(503);
        UUID jobId = jobId(importText(g, DOCUMENT));
        worker.drain();
        assertThat(jobRow(jobId)).containsEntry("status", "failed").containsEntry("error_code", "model_failed");
        assertThat(STUB.modelRequests()).hasSize(3);
        assertThat(revision(g)).isEqualTo(revision);

        STUB.reset();
        STUB.modelAnswers("{\"etapas\":[{\"nome\":\"Abertura\"}]}");
        STUB.modelAnswers(ANSWER.replace("{\"name\":\"Abertura\"}", "{\"name\":null}"));
        STUB.modelAnswers(ANSWER.replace("\"stages\":", "\"extra\":1,\"stages\":"));
        UUID outsideSchema = jobId(importText(g, DOCUMENT));
        worker.drain();
        assertThat(jobRow(outsideSchema)).containsEntry("status", "failed").containsEntry("error_code", "model_failed");
        assertThat(STUB.modelRequests()).hasSize(3);
        assertThat(revision(g)).isEqualTo(revision);
        assertThat(environment.getProperty("spring.ai.openai.timeout")).isEqualTo("120s");

        STUB.reset();
        STUB.modelAnswers("{\"stages\":null,\"discovery\":null,\"objections\":null,\"priceTable\":null}");
        STUB.modelAnswers(ANSWER.replace("\"stages\":[{\"name\":\"Abertura\"},{\"name\":\"Diagnóstico\"}]", "\"stages\":\"Abertura\""));
        STUB.modelAnswers(ANSWER.replaceFirst("\"plans\":\\[", "\"plans\":null,\"x\":["));
        UUID nullBlocks = jobId(importText(g, DOCUMENT));
        worker.drain();
        assertThat(jobRow(nullBlocks)).containsEntry("status", "failed").containsEntry("error_code", "model_failed");
        assertThat(STUB.modelRequests()).hasSize(3);

        STUB.reset();
        STUB.modelAnswers(ANSWER.replace("{\"name\":\"Abertura\"}", "{\"name\":123}"));
        STUB.modelAnswers(ANSWER.replace("\"installmentsCount\":6", "\"installmentsCount\":2.9"));
        STUB.modelAnswers(ANSWER.replace("\"maxDiscountExcerpt\":\"Desconto máximo na call: R$ 300.\",", ""));
        UUID wrongTypes = jobId(importText(g, DOCUMENT));
        worker.drain();
        assertThat(jobRow(wrongTypes)).containsEntry("status", "failed").containsEntry("error_code", "model_failed");
        assertThat(STUB.modelRequests()).hasSize(3);
        assertThat(revision(g)).isEqualTo(revision);

        STUB.reset();
        STUB.modelFails(500);
        STUB.modelFails(500);
        STUB.modelAnswers(ANSWER);
        UUID third = jobId(importText(g, DOCUMENT));
        worker.drain();
        assertThat(jobRow(third)).containsEntry("status", "done");

        STUB.reset();
        STUB.modelAnswers(ANSWER.replace("\"installmentsCount\":6", "\"installmentsCount\":6.0"));
        UUID integerWithDecimalPoint = jobId(importText(g, DOCUMENT));
        worker.drain();
        assertThat(jobRow(integerWithDecimalPoint)).containsEntry("status", "done");
        assertThat(STUB.modelRequests()).hasSize(1);
    }

    @Test
    @DisplayName("[C106] valor em dinheiro da resposta do modelo chega com todos os dígitos")
    void modelMoneyKeepsEveryDigit() {
        ExtractedPlaybook parsed = extraction.parse(ANSWER
                .replace("\"installmentsValue\":800", "\"installmentsValue\":90071992547409.91")
                .replace("\"cash\":4500", "\"cash\":4500.10"));
        ExtractedPlaybook.Plan plan = parsed.priceTable().plans().get(0);
        assertThat(plan.installmentsValue().toPlainString()).isEqualTo("90071992547409.91");
        assertThat(plan.cash().toPlainString()).isEqualTo("4500.10");
    }

    @Test
    @DisplayName("[C82] bloco acima do limite importa até o limite e avisa os itens de fora")
    void blockLimit() throws Exception {
        Gestor g = newGestor();
        UUID playbookId = playbookId(g);
        var stages = new StringBuilder();
        for (int i = 0; i < 29; i++) {
            stages.append(i == 0 ? "" : ",").append("{\"id\":\"").append(UUID.randomUUID()).append("\",\"name\":\"Etapa ").append(i).append("\"}");
        }
        save(g, playbookId, Drafts.complete().replaceFirst("\"stages\":\\[[^\\]]*\\]", "\"stages\":[" + stages + "]"), 0)
                .andExpect(status().isOk());
        STUB.modelAnswers(ANSWER.replace("\"stages\":[{\"name\":\"Abertura\"},{\"name\":\"Diagnóstico\"}]",
                "\"stages\":[{\"name\":\"A\"},{\"name\":\"B\"},{\"name\":\"C\"}]"));
        importText(g, DOCUMENT);
        worker.drain();

        JsonNode playbook = getPlaybook(g);
        assertThat(playbook.get("draft").get("stages").size()).isEqualTo(30);
        assertThat(playbook.get("draft").get("stages").get(29).get("name").asString()).isEqualTo("A");
        assertThat(playbook.get("importJob").get("counts").get("stages").asInt()).isEqualTo(1);
        assertThat(playbook.get("importJob").get("skipped").asInt()).isEqualTo(2);
    }

    @Test
    @DisplayName("[C83] conclusão que não marca a tarefa não grava linhas")
    void completionWithoutTheLeaseWritesNothing() throws Exception {
        Gestor g = newGestor();
        long revision = revision(g);
        UUID jobId = jobId(importText(g, DOCUMENT));
        var claim = claimJob(jobId);
        var job = tenancy.inBusiness(null, g.businessId(), () -> jobs.find(jobId)).orElseThrow();

        boolean finished = worker.complete(job, UUID.randomUUID(), extracted(), DOCUMENT);

        assertThat(finished).isFalse();
        assertThat(revision(g)).isEqualTo(revision);
        assertThat(jobRow(jobId)).containsEntry("status", "running").containsEntry("lease_token", claim.leaseToken());
    }

    @Test
    @DisplayName("[C84] execução com concessão vencida não conclui depois de outra assumir")
    void expiredExecutionCannotFinish() throws Exception {
        Gestor g = newGestor();
        UUID jobId = jobId(importText(g, DOCUMENT));
        var first = claimJob(jobId);
        expireLease(jobId);
        var second = claimJob(jobId);
        assertThat(second.leaseToken()).isNotEqualTo(first.leaseToken());
        STUB.modelAnswers(ANSWER);
        worker.process(second);

        var job = tenancy.inBusiness(null, g.businessId(), () -> jobs.find(jobId)).orElseThrow();
        assertThat(worker.complete(job, first.leaseToken(), extracted(), DOCUMENT)).isFalse();

        assertThat(getPlaybook(g).get("draft").get("stages").size()).isEqualTo(2);
        assertThat(jobRow(jobId)).containsEntry("status", "done");
    }

    @Test
    @DisplayName("[C85] concessão vencida é reprocessada sem duplicar linhas")
    void expiredLeaseIsReprocessedOnce() throws Exception {
        Gestor g = newGestor();
        UUID jobId = jobId(importText(g, DOCUMENT));
        claimJob(jobId);
        expireLease(jobId);
        STUB.modelAnswers(ANSWER);

        worker.drain();

        assertThat(jobRow(jobId)).containsEntry("status", "done").containsEntry("attempts", 2);
        assertThat(getPlaybook(g).get("draft").get("stages").size()).isEqualTo(2);
    }

    @Test
    @DisplayName("[C86] claim entrega tarefas de dois negócios e o papel app só vê as suas")
    void claimSeesAllBusinessesAppSeesOne() throws Exception {
        Gestor a = newGestor();
        Gestor b = newGestor();
        UUID jobA = jobId(importText(a, DOCUMENT));
        UUID jobB = jobId(importText(b, DOCUMENT));

        var first = jobs.claim().orElseThrow();
        var second = jobs.claim().orElseThrow();
        assertThat(List.of(first.jobId(), second.jobId())).containsExactlyInAnyOrder(jobA, jobB);
        assertThat(List.of(first.businessId(), second.businessId())).containsExactlyInAnyOrder(a.businessId(), b.businessId());

        var app = TestDatabase.appRole();
        var jdbc = JdbcClient.create(app);
        new TransactionTemplate(new DataSourceTransactionManager(app)).executeWithoutResult(status -> {
            jdbc.sql("SELECT set_config('app.business_id', ?, true)").param(a.businessId().toString()).query().singleValue();
            assertThat(jdbc.sql("SELECT DISTINCT business_id FROM import_jobs").query(UUID.class).list())
                    .containsExactly(a.businessId());
        });
    }

    @Test
    @DisplayName("[C117] tabela temporária import_jobs do papel app não substitui a real nas funções privilegiadas")
    void definerFunctionsIgnoreTemporaryTables() {
        UUID fake = UUID.randomUUID();
        var app = TestDatabase.appRole();
        var jdbc = JdbcClient.create(app);
        new TransactionTemplate(new DataSourceTransactionManager(app)).executeWithoutResult(status -> {
            jdbc.sql("""
                    CREATE TEMP TABLE import_jobs (id uuid, business_id uuid, status text, lease_token uuid,
                        lease_expires_at timestamptz, attempts int, created_at timestamptz) ON COMMIT DROP""").update();
            jdbc.sql("INSERT INTO pg_temp.import_jobs VALUES (?, ?, 'queued', null, null, 0, now())")
                    .params(fake, UUID.randomUUID()).update();

            List<UUID> claimed = jdbc.sql("SELECT job_id FROM claim_next_import_job(600)").query(UUID.class).list();
            assertThat(claimed).doesNotContain(fake);
            assertThat(jdbc.sql("SELECT job_id FROM import_job_statuses(ARRAY[?]::uuid[])").param(fake)
                    .query(UUID.class).list()).isEmpty();
            assertThat(jdbc.sql("SELECT status FROM pg_temp.import_jobs WHERE id = ?").param(fake)
                    .query(String.class).single()).isEqualTo("queued");
            status.setRollbackOnly();
        });
    }

    @Test
    @DisplayName("[C118] GET do playbook lê rascunho e tarefa no mesmo snapshot mesmo com a importação concluindo no meio")
    void getReadsOneSnapshot() throws Exception {
        Gestor g = newGestor();
        UUID jobId = jobId(importText(g, DOCUMENT));
        long revision = revision(g);
        var url = TestDatabase.POSTGRES.getJdbcUrl();
        try (var worker = java.sql.DriverManager.getConnection(url, TestDatabase.POSTGRES.getUsername(),
                TestDatabase.POSTGRES.getPassword())) {
            worker.setAutoCommit(false);
            // Segura a tabela de tarefas: o GET lê o rascunho e para na leitura da tarefa.
            worker.createStatement().execute("LOCK TABLE sales_copilot.import_jobs IN ACCESS EXCLUSIVE MODE");
            var response = java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                try {
                    return getPlaybook(g);
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            });
            long deadline = System.currentTimeMillis() + 10_000;
            while (TestDatabase.admin().sql("""
                    SELECT count(*) FROM pg_stat_activity
                     WHERE usename = 'app' AND wait_event_type = 'Lock' AND query ILIKE '%import_jobs%'""")
                    .query(Long.class).single() == 0) {
                assertThat(System.currentTimeMillis()).as("GET esperando a tarefa").isLessThan(deadline);
                Thread.sleep(50);
            }
            // A conclusão da importação acontece entre as duas leituras do GET.
            try (var update = worker.prepareStatement("""
                    UPDATE sales_copilot.playbooks SET draft = jsonb_set(draft, '{stages}',
                        '[{"id":"00000000-0000-0000-0000-000000000001","name":"Abertura"}]'), draft_revision = draft_revision + 1
                     WHERE business_id = ?""")) {
                update.setObject(1, g.businessId());
                update.executeUpdate();
            }
            try (var done = worker.prepareStatement("""
                    UPDATE sales_copilot.import_jobs SET status = 'done', finished_at = now(),
                        counts = '{"stages":1,"discovery":0,"objections":0,"plans":0}' WHERE id = ?""")) {
                done.setObject(1, jobId);
                done.executeUpdate();
            }
            worker.commit();

            JsonNode playbook = response.get(10, java.util.concurrent.TimeUnit.SECONDS);
            assertThat(playbook.get("importJob").get("status").asString()).isEqualTo("queued");
            assertThat(playbook.get("revision").asLong()).isEqualTo(revision);
        }
        JsonNode after = getPlaybook(g);
        assertThat(after.get("importJob").get("status").asString()).isEqualTo("done");
        assertThat(after.get("revision").asLong()).isEqualTo(revision + 1);
    }

    @Test
    @DisplayName("[C87] arquivo da tarefa é apagado depois do commit e o texto não é gravado")
    void fileIsDeletedAndTextIsNotStored() throws Exception {
        Gestor g = newGestor();
        STUB.modelAnswers(ANSWER);
        UUID jobId = jobId(importText(g, DOCUMENT + "\nMARCADOR-DO-DOCUMENTO"));
        String file = (String) jobRow(jobId).get("file_path");
        assertThat(Files.exists(IMPORT_DIRECTORY.resolve(file))).isTrue();

        worker.drain();

        assertThat(Files.exists(IMPORT_DIRECTORY.resolve(file))).isFalse();
        String row = admin.sql("SELECT row_to_json(j)::text FROM import_jobs j WHERE id = ?").param(jobId).query(String.class).single();
        assertThat(row).doesNotContain("MARCADOR-DO-DOCUMENTO");
        String draft = admin.sql("SELECT draft::text FROM playbooks WHERE business_id = ?").param(g.businessId()).query(String.class).single();
        assertThat(draft).doesNotContain("MARCADOR-DO-DOCUMENTO");

        // Um .docx passa pelo processo de extração: o texto volta pela memória e nenhum arquivo o contém,
        // nem durante a extração. Uma vigia lê os arquivos da pasta de importação e das pastas de extração.
        byte[] marked;
        try (var document = new org.apache.poi.xwpf.usermodel.XWPFDocument(); var out = new ByteArrayOutputStream()) {
            document.createParagraph().createRun().setText("Etapas: Abertura. MARCADOR-EM-DOCX");
            document.write(out);
            marked = out.toByteArray();
        }
        STUB.modelAnswers(ANSWER);
        UUID docxJob = jobId(importFile(g, new MockMultipartFile("file", "marcado.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", marked)));
        var seen = new java.util.concurrent.atomic.AtomicReference<String>();
        var watching = new java.util.concurrent.atomic.AtomicBoolean(true);
        Thread watcher = Thread.ofVirtual().start(() -> {
            List<Path> roots = List.of(IMPORT_DIRECTORY, Path.of(System.getProperty("java.io.tmpdir")));
            while (watching.get()) {
                for (Path root : roots) {
                    try (var paths = Files.walk(root, root.equals(IMPORT_DIRECTORY) ? 4 : 1)) {
                        paths.filter(path -> root.equals(IMPORT_DIRECTORY) || path.getFileName().toString().startsWith("extracao-"))
                                .flatMap(path -> {
                                    try {
                                        return Files.isDirectory(path) ? Files.walk(path) : java.util.stream.Stream.of(path);
                                    } catch (java.io.IOException e) {
                                        return java.util.stream.Stream.empty();
                                    }
                                })
                                .filter(Files::isRegularFile)
                                .forEach(path -> {
                                    try {
                                        if (new String(Files.readAllBytes(path), java.nio.charset.StandardCharsets.UTF_8).contains("MARCADOR-EM-DOCX")) {
                                            seen.set(path.toString());
                                        }
                                    } catch (java.io.IOException | RuntimeException e) {
                                        // O arquivo sumiu entre a listagem e a leitura.
                                    }
                                });
                    } catch (java.io.IOException | RuntimeException e) {
                        // A pasta mudou durante a listagem; a próxima volta confere de novo.
                    }
                }
            }
        });
        worker.drain();
        watching.set(false);
        watcher.join();
        assertThat(jobRow(docxJob)).containsEntry("status", "done");
        assertThat(seen.get()).as("arquivo com o texto extraído").isNull();
        try (var left = Files.list(IMPORT_DIRECTORY)) {
            assertThat(left.map(path -> path.getFileName().toString())).noneMatch(name -> name.startsWith("extracao-"));
        }

        UUID failing = jobId(importText(g, "x".repeat(100_001)));
        String failingFile = (String) jobRow(failing).get("file_path");
        worker.drain();
        assertThat(jobRow(failing)).containsEntry("status", "failed");
        assertThat(Files.exists(IMPORT_DIRECTORY.resolve(failingFile))).isFalse();
    }

    @Test
    @DisplayName("[C88] varredura apaga arquivos encerrados e órfãos com mais de 1 hora")
    void sweeperDeletesFinishedAndOldOrphans() throws Exception {
        Gestor g = newGestor();
        UUID finished = jobId(importText(g, DOCUMENT));
        admin.sql("UPDATE import_jobs SET status = 'done' WHERE id = ?").param(finished).update();
        Path finishedFile = IMPORT_DIRECTORY.resolve((String) jobRow(finished).get("file_path"));
        UUID active = jobId(importText(g, DOCUMENT));
        Path activeFile = IMPORT_DIRECTORY.resolve((String) jobRow(active).get("file_path"));
        Path oldOrphan = Files.writeString(IMPORT_DIRECTORY.resolve(UUID.randomUUID() + ".txt"), "órfão");
        Files.setLastModifiedTime(oldOrphan, FileTime.from(Instant.now().minus(Duration.ofMinutes(61))));
        Path recentOrphan = Files.writeString(IMPORT_DIRECTORY.resolve(UUID.randomUUID() + ".txt"), "órfão recente");

        sweeper.sweep();

        assertThat(finishedFile).doesNotExist();
        assertThat(oldOrphan).doesNotExist();
        assertThat(activeFile).exists();
        assertThat(recentOrphan).exists();
    }

    @Test
    @DisplayName("[C89] chamada ao OpenRouter leva provider.zdr true")
    void everyModelCallRequiresZeroDataRetention() throws Exception {
        Gestor g = newGestor();
        STUB.modelFails(500);
        STUB.modelAnswers(ANSWER);
        importText(g, DOCUMENT);
        worker.drain();

        assertThat(STUB.modelRequests()).hasSize(2);
        for (String request : STUB.modelRequests()) {
            JsonNode body = json.readTree(request);
            assertThat(body.get("provider").get("zdr").asBoolean()).isTrue();
        }
    }

    @Test
    @DisplayName("[C43] gravação e importação simultâneas nunca gravam depois da tarefa")
    void saveWaitsForTheImportLock() throws Exception {
        Gestor g = newGestor();
        UUID playbookId = playbookId(g);
        UUID userId = userId(g.email());
        var locked = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var enqueue = CompletableFuture.runAsync(() -> tenancy.runInBusiness(userId, g.businessId(), () -> {
            drafts.lock(playbookId);
            jobs.insert(new ImportJobs.Job(UUID.randomUUID(), g.businessId(), playbookId, "queued", "text", null, null));
            locked.countDown();
            try {
                release.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }));
        assertThat(locked.await(10, TimeUnit.SECONDS)).isTrue();

        var saving = CompletableFuture.supplyAsync(() -> {
            try {
                return save(g, playbookId, Drafts.complete(), 0).andReturn().getResponse();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        });
        Thread.sleep(500);
        assertThat(saving).isNotDone();

        release.countDown();
        enqueue.get(10, TimeUnit.SECONDS);
        var response = saving.get(10, TimeUnit.SECONDS);
        assertThat(response.getStatus()).isEqualTo(409);
        assertThat(json.readTree(response.getContentAsString()).get("code").asString()).isEqualTo("import_running");
        assertThat(revision(g)).isZero();

        // Pelo endpoint real: com o playbook travado por outra transação, o POST de importação espera a trava
        // e só cria a tarefa depois que ela é liberada.
        Gestor other = newGestor();
        UUID otherPlaybook = playbookId(other);
        try (var holder = java.sql.DriverManager.getConnection(TestDatabase.POSTGRES.getJdbcUrl(),
                TestDatabase.POSTGRES.getUsername(), TestDatabase.POSTGRES.getPassword())) {
            holder.setAutoCommit(false);
            try (var lock = holder.prepareStatement("SELECT id FROM sales_copilot.playbooks WHERE id = ? FOR UPDATE")) {
                lock.setObject(1, otherPlaybook);
                lock.executeQuery().close();
            }
            var importing = CompletableFuture.supplyAsync(() -> {
                try {
                    return importText(other, DOCUMENT).andReturn().getResponse().getStatus();
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            });
            long deadline = System.currentTimeMillis() + 10_000;
            while (TestDatabase.admin().sql("""
                    SELECT count(*) FROM pg_stat_activity
                     WHERE usename = 'app' AND wait_event_type = 'Lock' AND query ILIKE '%playbooks%'""")
                    .query(Long.class).single() == 0) {
                assertThat(importing).as("POST de importação esperando a trava").isNotDone();
                assertThat(System.currentTimeMillis()).as("POST de importação esperando a trava").isLessThan(deadline);
                Thread.sleep(20);
            }
            assertThat(TestDatabase.admin().sql("SELECT count(*) FROM import_jobs WHERE playbook_id = ?").param(otherPlaybook)
                    .query(Long.class).single()).isZero();
            holder.commit();
            assertThat(importing.get(10, TimeUnit.SECONDS)).isEqualTo(202);
        }
        assertThat(TestDatabase.admin().sql("SELECT count(*) FROM import_jobs WHERE playbook_id = ?").param(otherPlaybook)
                .query(Long.class).single()).isEqualTo(1);
    }

    private ResultActions importText(Gestor g, String text) throws Exception {
        String body = json.writeValueAsString(java.util.Map.of("playbookId", playbookId(g).toString(), "text", text));
        return mvc.perform(post("/api/businesses/" + g.businessId() + "/playbook/imports").cookie(g.session()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions importUrl(Gestor g, String url) throws Exception {
        String body = json.writeValueAsString(java.util.Map.of("playbookId", playbookId(g).toString(), "googleDocsUrl", url));
        return mvc.perform(post("/api/businesses/" + g.businessId() + "/playbook/imports").cookie(g.session()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions importFile(Gestor g, MockMultipartFile file) throws Exception {
        return mvc.perform(multipart("/api/businesses/" + g.businessId() + "/playbook/imports").file(file)
                .param("playbookId", playbookId(g).toString()).cookie(g.session()).with(csrf()));
    }

    private ResultActions save(Gestor g, UUID playbookId, String draft, long revision) throws Exception {
        return mvc.perform(put("/api/businesses/" + g.businessId() + "/playbook/draft").cookie(g.session()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(Drafts.saveBody(playbookId, draft, revision)));
    }

    private UUID jobId(ResultActions result) throws Exception {
        return UUID.fromString(json.readTree(result.andExpect(status().isAccepted()).andReturn().getResponse()
                .getContentAsString()).get("jobId").asString());
    }

    private UUID playbookId(Gestor g) throws Exception {
        return UUID.fromString(getPlaybook(g).get("playbookId").asString());
    }

    private long revision(Gestor g) throws Exception {
        return getPlaybook(g).get("revision").asLong();
    }

    /** Pega a tarefa dada, devolvendo à fila as outras que o claim entregar antes. */
    private ImportJobs.Claim claimJob(UUID jobId) {
        for (int i = 0; i < 20; i++) {
            var claim = jobs.claim().orElseThrow();
            if (claim.jobId().equals(jobId)) {
                return claim;
            }
            admin.sql("UPDATE import_jobs SET status = 'failed' WHERE id = ?").param(claim.jobId()).update();
        }
        throw new AssertionError("tarefa não entregue pelo claim");
    }

    private void expireLease(UUID jobId) {
        admin.sql("UPDATE import_jobs SET lease_expires_at = now() - interval '1 minute' WHERE id = ?").param(jobId).update();
    }

    /** Um .docx com tabela e o número de caracteres de texto pedido, para medir o limite sobre o texto e não sobre o XHTML. */
    private static byte[] docx(int characters) throws java.io.IOException {
        try (var document = new org.apache.poi.xwpf.usermodel.XWPFDocument(); var out = new ByteArrayOutputStream()) {
            var table = document.createTable(1, 2);
            table.getRow(0).getCell(0).setText("Residencial");
            table.getRow(0).getCell(1).setText("R$ 4.500");
            int remaining = characters - "Residencial".length() - "R$ 4.500".length();
            while (remaining > 0) {
                int size = Math.min(remaining, 1_000);
                document.createParagraph().createRun().setText("a".repeat(size));
                remaining -= size;
            }
            document.write(out);
            return out.toByteArray();
        }
    }

    /** Metadados do documento ficam fora do corpo e não contam no limite de texto. */
    private static byte[] docxWithTitle(String title) throws java.io.IOException {
        try (var document = new org.apache.poi.xwpf.usermodel.XWPFDocument(); var out = new ByteArrayOutputStream()) {
            document.getProperties().getCoreProperties().setTitle(title);
            document.createParagraph().createRun().setText("Abertura");
            document.write(out);
            return out.toByteArray();
        }
    }

    private static ExtractedPlaybook extracted() {
        return new ExtractedPlaybook(List.of(new ExtractedPlaybook.Stage("Abertura")), List.of(), List.of(), null);
    }
}
