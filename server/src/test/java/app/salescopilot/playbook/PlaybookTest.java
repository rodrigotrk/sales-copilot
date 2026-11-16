package app.salescopilot.playbook;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.support.TransactionTemplate;

import app.salescopilot.support.Drafts;
import app.salescopilot.support.IntegrationTest;
import app.salescopilot.support.TestDatabase;
import tools.jackson.databind.JsonNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PlaybookTest extends IntegrationTest {

    @Test
    @DisplayName("[C37a] texto acima de 120 ou 500 caracteres responde 422 com o campo")
    void textLimits() throws Exception {
        Gestor g = newGestor();
        UUID id = playbookId(g);
        UUID stage = UUID.randomUUID();
        String draft = empty().replace("\"stages\":[]", "\"stages\":[{\"id\":\"" + stage + "\",\"name\":\"" + "a".repeat(121) + "\"}]");
        save(g, id, draft, 0).andExpect(status().isUnprocessableContent())
                .andExpect(header().string("Content-Type", "application/problem+json"))
                .andExpect(jsonPath("$.errors[0].itemId").value(stage.toString()))
                .andExpect(jsonPath("$.errors[0].field").value("name"))
                .andExpect(jsonPath("$.errors[0].message").value("Máximo de 120 caracteres."));
        String question = empty().replace("\"discovery\":[]", "\"discovery\":[{\"id\":\"" + UUID.randomUUID()
                + "\",\"information\":\"x\",\"question\":\"" + "q".repeat(501) + "\"}]");
        save(g, id, question, 0).andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors[0].message").value("Máximo de 500 caracteres."));
        save(g, id, question.replace("q".repeat(501), "q".repeat(500)), 0).andExpect(status().isOk());
    }

    @Test
    @DisplayName("[C96] rascunho nulo ou com item nulo responde 422 invalid_document sem gravar")
    void nullDraftIsRefused() throws Exception {
        Gestor g = newGestor();
        UUID id = playbookId(g);
        mvc.perform(put("/api/businesses/" + g.businessId() + "/playbook/draft").cookie(g.session()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"playbookId\":\"" + id + "\",\"draft\":null,\"revision\":0}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(header().string("Content-Type", "application/problem+json"))
                .andExpect(jsonPath("$.errors[0].code").value("invalid_document"));
        save(g, id, empty().replace("\"stages\":[]", "\"stages\":[null]"), 0)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors[0].code").value("invalid_document"));
        save(g, id, empty().replace("\"plans\":[]", "\"plans\":[{\"id\":null,\"name\":\"Plano\",\"extraValue\":null,"
                + "\"cash\":\"0.00\",\"installments\":{\"count\":1,\"value\":\"0.00\"}}]"), 0)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors[*].code", org.hamcrest.Matchers.containsInAnyOrder(
                        "invalid_id", "invalid_cash", "invalid_installments")));
        assertThat(playbook(g).get("revision").asLong()).isZero();
    }

    @Test
    @DisplayName("[C107] desconto sem plano fica guardado e só bloqueia a publicação quando há plano")
    void discountWithoutPlans() throws Exception {
        Gestor g = newGestor();
        UUID id = playbookId(g);
        var draft = (tools.jackson.databind.node.ObjectNode) json.readTree(Drafts.complete());
        var table = (tools.jackson.databind.node.ObjectNode) draft.get("priceTable");
        var plans = table.get("plans").deepCopy();
        table.putArray("plans");
        table.putObject("maxDiscountReview").put("status", "pending").put("excerpt", "Desconto máximo: 300.");
        save(g, id, json.writeValueAsString(draft), 0).andExpect(status().isOk())
                .andExpect(jsonPath("$.draft.priceTable.maxDiscount").value("300.00"))
                .andExpect(jsonPath("$.draft.priceTable.maxDiscountReview.status").value("pending"));

        publish(g, id, 1).andExpect(status().isCreated());
        // A versão sai sem o desconto escondido e sem a marca (AD-062); o rascunho continua com os dois.
        JsonNode version = json.readTree(TestDatabase.admin()
                .sql("SELECT content::text FROM playbook_versions WHERE playbook_id = ? AND number = 1").param(id)
                .query(String.class).single());
        assertThat(version.get("priceTable").get("maxDiscount").isNull()).isTrue();
        assertThat(version.get("priceTable").get("maxDiscountReview").isNull()).isTrue();
        JsonNode current = playbook(g);
        assertThat(current.get("draft").get("priceTable").get("maxDiscount").asString()).isEqualTo("300.00");
        assertThat(current.get("hasUnpublishedChanges").asBoolean()).isFalse();
        publish(g, id, 1).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("nothing_to_publish"));

        table.set("plans", plans);
        save(g, id, json.writeValueAsString(draft), 1).andExpect(status().isOk());
        publish(g, id, 2).andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("pending_review"));
    }

    @Test
    @DisplayName("[C114] número de parcelas fracionário é recusado sem gravar e 6.0 vale 6")
    void fractionalInstallmentCountIsRefused() throws Exception {
        Gestor g = newGestor();
        UUID id = playbookId(g);
        String fractional = Drafts.complete().replace("\"count\":6,", "\"count\":60.9,");
        assertThat(fractional).contains("60.9");
        save(g, id, fractional, 0).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("malformed_request"));
        assertThat(playbook(g).get("revision").asLong()).isZero();

        save(g, id, Drafts.complete().replace("\"count\":6,", "\"count\":6.0,"), 0).andExpect(status().isOk())
                .andExpect(jsonPath("$.draft.priceTable.plans[0].installments.count").value(6));
    }

    @Test
    @DisplayName("[C128] valor em dinheiro com expoente enorme responde 422 com erro de campo, sem 500 nem gravar")
    void hugeMoneyExponentIsRefused() throws Exception {
        Gestor g = newGestor();
        UUID id = playbookId(g);
        var draft = (tools.jackson.databind.node.ObjectNode) json.readTree(Drafts.complete());
        var plan = (tools.jackson.databind.node.ObjectNode) draft.get("priceTable").get("plans").get(0);
        ((tools.jackson.databind.node.ObjectNode) plan.get("installments")).put("value", "1E+2147483647");
        save(g, id, json.writeValueAsString(draft), 0).andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors[0].field").value("installments"));

        draft = (tools.jackson.databind.node.ObjectNode) json.readTree(Drafts.complete());
        ((tools.jackson.databind.node.ObjectNode) draft.get("priceTable").get("plans").get(0)).put("cash", "1E-2147483647");
        save(g, id, json.writeValueAsString(draft), 0).andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors[0].field").value("cash"));

        draft = (tools.jackson.databind.node.ObjectNode) json.readTree(Drafts.complete());
        var table = (tools.jackson.databind.node.ObjectNode) draft.get("priceTable");
        table.putArray("plans");
        table.put("maxDiscount", "1E+100000000");
        save(g, id, json.writeValueAsString(draft), 0).andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors[0].field").value("maxDiscount"));
        assertThat(playbook(g).get("revision").asLong()).isZero();

        // Um valor grande de verdade, sem o teto do preço à vista, ainda vale para a parcela.
        draft = (tools.jackson.databind.node.ObjectNode) json.readTree(Drafts.complete());
        plan = (tools.jackson.databind.node.ObjectNode) draft.get("priceTable").get("plans").get(0);
        ((tools.jackson.databind.node.ObjectNode) plan.get("installments")).put("value", "999999999999.99");
        save(g, id, json.writeValueAsString(draft), 0).andExpect(status().isOk());
    }

    @Test
    @DisplayName("[C38a] rascunho acima do limite de itens responde 422")
    void itemLimits() throws Exception {
        Gestor g = newGestor();
        save(g, playbookId(g), empty().replace("\"stages\":[]", "\"stages\":[" + stages(31) + "]"), 0)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors[0].code").value("too_many_items"))
                .andExpect(jsonPath("$.errors[0].message").value("Limite de 30 itens neste bloco."));
        save(g, playbookId(g), empty().replace("\"stages\":[]", "\"stages\":[" + stages(30) + "]"), 0)
                .andExpect(status().isOk());
        String discovery = items(51, i -> "{\"id\":\"" + UUID.randomUUID() + "\",\"information\":\"I" + i + "\",\"question\":\"Q\"}");
        save(g, playbookId(g), empty().replace("\"discovery\":[]", "\"discovery\":[" + discovery + "]"), 1)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors[0].message").value("Limite de 50 itens neste bloco."));
        String objections = items(51, i -> "{\"id\":\"" + UUID.randomUUID() + "\",\"name\":\"O" + i + "\",\"leadSays\":\"\",\"approvedResponse\":\"R\"}");
        save(g, playbookId(g), empty().replace("\"objections\":[]", "\"objections\":[" + objections + "]"), 1)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors[0].message").value("Limite de 50 itens neste bloco."));
        String plans = items(21, i -> "{\"id\":\"" + UUID.randomUUID() + "\",\"name\":\"P" + i
                + "\",\"extraValue\":\"\",\"cash\":\"100.00\",\"installments\":null,\"review\":null}");
        save(g, playbookId(g), empty().replace("\"plans\":[]", "\"plans\":[" + plans + "]"), 1)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors[0].message").value("Limite de 20 itens neste bloco."));
        save(g, playbookId(g), empty().replace("\"plans\":[]", "\"plans\":[" + plans.substring(0, plans.lastIndexOf(",{")) + "]"), 1)
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("[C41a] gravação com revisão antiga responde 409 revision_conflict")
    void staleRevisionIsRefused() throws Exception {
        Gestor g = newGestor();
        UUID id = playbookId(g);
        save(g, id, Drafts.complete(), 0).andExpect(status().isOk()).andExpect(jsonPath("$.revision").value(1));
        save(g, id, empty(), 0).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("revision_conflict"));
        assertThat(playbook(g).get("draft").get("stages").size()).isEqualTo(2);
    }

    @Test
    @DisplayName("[C42a] gravação com importação ativa responde 409 import_running")
    void importRunningBlocksSaving() throws Exception {
        Gestor g = newGestor();
        UUID id = playbookId(g);
        admin.sql("INSERT INTO import_jobs (id, business_id, playbook_id, status, source_kind) VALUES (?, ?, ?, 'queued', 'text')")
                .params(UUID.randomUUID(), g.businessId(), id).update();
        save(g, id, Drafts.complete(), 0).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("import_running"));
        publish(g, id, 0).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("import_running"));
    }

    @Test
    @DisplayName("[C54a] publicar cria a versão N+1 começando em 1")
    void publishCreatesNextVersion() throws Exception {
        Gestor g = newGestor();
        UUID id = playbookId(g);
        save(g, id, Drafts.complete(), 0);
        publish(g, id, 1).andExpect(status().isCreated()).andExpect(jsonPath("$.number").value(1));
        JsonNode after = playbook(g);
        assertThat(after.get("latestVersion").get("number").asInt()).isEqualTo(1);
        assertThat(after.get("hasUnpublishedChanges").asBoolean()).isFalse();

        save(g, id, Drafts.complete(), 1);
        assertThat(playbook(g).get("hasUnpublishedChanges").asBoolean()).isTrue();
        publish(g, id, 2).andExpect(status().isCreated()).andExpect(jsonPath("$.number").value(2));
    }

    @Test
    @DisplayName("[C55a] publicar com revisão antiga responde 409 e não cria versão")
    void publishWithStaleRevision() throws Exception {
        Gestor g = newGestor();
        UUID id = playbookId(g);
        save(g, id, Drafts.complete(), 0);
        save(g, id, Drafts.complete(), 1);
        publish(g, id, 1).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("revision_conflict"));
        assertThat(versions(id)).isZero();
    }

    @Test
    @DisplayName("[C56a] sem etapa, informação ou objeção responde 422 publish_minimum e tabela vazia publica")
    void publishMinimum() throws Exception {
        Gestor g = newGestor();
        UUID id = playbookId(g);
        publish(g, id, 0).andExpect(status().isUnprocessableContent()).andExpect(jsonPath("$.code").value("publish_minimum"))
                .andExpect(jsonPath("$.detail").value(
                        "Para publicar, cadastre ao menos uma etapa, uma informação a descobrir e uma objeção."));
        String withoutObjection = Drafts.complete().replaceAll("\"objections\":\\[.*?\\],\n", "\"objections\":[],\n");
        save(g, id, withoutObjection, 0);
        publish(g, id, 1).andExpect(jsonPath("$.code").value("publish_minimum"));
        save(g, id, Drafts.complete().replaceAll("\"stages\":\\[[^\\]]*\\]", "\"stages\":[]"), 1);
        publish(g, id, 2).andExpect(jsonPath("$.code").value("publish_minimum"));
        save(g, id, Drafts.complete().replaceAll("\"discovery\":\\[[^\\]]*\\]", "\"discovery\":[]"), 2);
        publish(g, id, 3).andExpect(jsonPath("$.code").value("publish_minimum"));

        String noPrices = Drafts.complete().replaceAll("\"plans\":\\[[\\s\\S]*\\]\\}\\}", "\"plans\":[]}}")
                .replace("\"maxDiscount\":\"300.00\"", "\"maxDiscount\":null");
        save(g, id, noPrices, 3).andExpect(status().isOk());
        publish(g, id, 4).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("[C57a] linha incompleta responde 422 incomplete_rows com os ids")
    void incompleteRows() throws Exception {
        Gestor g = newGestor();
        UUID id = playbookId(g);
        UUID blankStage = UUID.randomUUID();
        UUID planWithoutCash = UUID.randomUUID();
        String draft = Drafts.complete()
                .replace("\"stages\":[", "\"stages\":[{\"id\":\"" + blankStage + "\",\"name\":\"\"},")
                .replace("\"plans\":[", "\"plans\":[{\"id\":\"" + planWithoutCash
                        + "\",\"name\":\"Sem preço\",\"extraValue\":\"\",\"cash\":null,\"installments\":null,\"review\":null},");
        save(g, id, draft, 0).andExpect(status().isOk());
        publish(g, id, 1).andExpect(status().isUnprocessableContent()).andExpect(jsonPath("$.code").value("incomplete_rows"))
                .andExpect(jsonPath("$.errors.length()").value(2))
                .andExpect(jsonPath("$.errors[0].itemId").value(blankStage.toString()))
                .andExpect(jsonPath("$.errors[1].itemId").value(planWithoutCash.toString()));

        UUID noQuestion = UUID.randomUUID();
        UUID noResponse = UUID.randomUUID();
        String others = Drafts.complete()
                .replace("\"discovery\":[", "\"discovery\":[{\"id\":\"" + noQuestion + "\",\"information\":\"Prazo\",\"question\":\"\"},")
                .replace("\"objections\":[", "\"objections\":[{\"id\":\"" + noResponse
                        + "\",\"name\":\"Tempo\",\"leadSays\":\"Agora não\",\"approvedResponse\":\" \"},");
        save(g, id, others, 1).andExpect(status().isOk());
        publish(g, id, 2).andExpect(jsonPath("$.code").value("incomplete_rows"))
                .andExpect(jsonPath("$.errors.length()").value(2))
                .andExpect(jsonPath("$.errors[0].itemId").value(noQuestion.toString()))
                .andExpect(jsonPath("$.errors[1].itemId").value(noResponse.toString()));

        save(g, id, Drafts.complete().replace("\"leadSays\":\"Está caro\"", "\"leadSays\":\"\""), 2).andExpect(status().isOk());
        publish(g, id, 3).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("[C58] desconto fora da regra responde 422 discount_invalid")
    void invalidDiscountBlocksPublishing() throws Exception {
        Gestor g = newGestor();
        UUID id = playbookId(g);
        save(g, id, Drafts.complete(), 0);
        // O rascunho só chega a esse estado pela importação, que não confere o desconto contra os planos.
        admin.sql("UPDATE playbooks SET draft = jsonb_set(draft, '{priceTable,maxDiscount}', '\"4500.00\"') WHERE id = ?")
                .param(id).update();
        publish(g, id, 1).andExpect(status().isUnprocessableContent()).andExpect(jsonPath("$.code").value("discount_invalid"))
                .andExpect(jsonPath("$.detail").value("O desconto máximo precisa ser menor que o menor preço à vista (R$ 4.500)."));
    }

    @Test
    @DisplayName("[C59a] valor marcado a conferir responde 422 pending_review")
    void pendingReviewBlocksPublishing() throws Exception {
        Gestor g = newGestor();
        UUID id = playbookId(g);
        save(g, id, Drafts.complete().replaceFirst("\"review\":null",
                java.util.regex.Matcher.quoteReplacement("\"review\":{\"status\":\"pending\",\"excerpt\":\"R$ 4.500\"}")), 0);
        String flaggedPlan = playbook(g).get("draft").get("priceTable").get("plans").get(0).get("id").asString();
        publish(g, id, 1).andExpect(status().isUnprocessableContent()).andExpect(jsonPath("$.code").value("pending_review"))
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].itemId").value(flaggedPlan));
        save(g, id, Drafts.complete().replace("\"maxDiscountReview\":null",
                "\"maxDiscountReview\":{\"status\":\"pending\",\"excerpt\":\"desconto de R$ 300\"}"), 1);
        publish(g, id, 2).andExpect(jsonPath("$.code").value("pending_review"));
        assertThat(versions(id)).isZero();
    }

    @Test
    @DisplayName("[C61a] rascunho igual à versão responde 409 nothing_to_publish")
    void nothingToPublish() throws Exception {
        Gestor g = newGestor();
        UUID id = playbookId(g);
        save(g, id, Drafts.complete(), 0);
        publish(g, id, 1).andExpect(status().isCreated());
        publish(g, id, 1).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("nothing_to_publish"));
        assertThat(versions(id)).isEqualTo(1);
    }

    @Test
    @DisplayName("[C63] UPDATE e DELETE em versões falham para o papel app")
    void versionsAreImmutable() throws Exception {
        Gestor g = newGestor();
        UUID id = playbookId(g);
        save(g, id, Drafts.complete(), 0);
        publish(g, id, 1);
        var app = TestDatabase.appRole();
        var jdbc = JdbcClient.create(app);
        var transactions = new TransactionTemplate(new DataSourceTransactionManager(app));
        for (String sql : List.of("UPDATE playbook_versions SET number = 99", "DELETE FROM playbook_versions")) {
            assertThatThrownBy(() -> transactions.executeWithoutResult(status -> {
                jdbc.sql("SELECT set_config('app.business_id', ?, true)").param(g.businessId().toString()).query().singleValue();
                jdbc.sql(sql).update();
            })).rootCause().hasMessageContaining("permission denied");
        }
        assertThat(versions(id)).isEqualTo(1);
    }

    @Test
    @DisplayName("[C64] versão nova mantém os ids dos itens")
    void versionsKeepItemIds() throws Exception {
        Gestor g = newGestor();
        UUID id = playbookId(g);
        String draft = Drafts.complete();
        save(g, id, draft, 0);
        publish(g, id, 1);
        JsonNode first = versionContent(id, 1);
        JsonNode saved = playbook(g).get("draft");
        assertThat(first).isEqualTo(saved);

        UUID added = UUID.randomUUID();
        save(g, id, draft.replace("\"stages\":[", "\"stages\":[{\"id\":\"" + added + "\",\"name\":\"Fechamento\"},"), 1);
        publish(g, id, 2);
        JsonNode second = versionContent(id, 2);
        assertThat(second.get("stages").get(1).get("id")).isEqualTo(first.get("stages").get(0).get("id"));
        assertThat(second.get("priceTable").get("plans").get(0).get("id")).isEqualTo(first.get("priceTable").get("plans").get(0).get("id"));
    }

    private ResultActions save(Gestor g, UUID playbookId, String draft, long revision) throws Exception {
        return mvc.perform(put("/api/businesses/" + g.businessId() + "/playbook/draft").cookie(g.session()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(Drafts.saveBody(playbookId, draft, revision)));
    }

    private ResultActions publish(Gestor g, UUID playbookId, long revision) throws Exception {
        return mvc.perform(post("/api/businesses/" + g.businessId() + "/playbook/versions").cookie(g.session()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(Drafts.publishBody(playbookId, revision)));
    }

    private JsonNode playbook(Gestor g) throws Exception {
        return getPlaybook(g);
    }

    private UUID playbookId(Gestor g) throws Exception {
        return UUID.fromString(getPlaybook(g).get("playbookId").asString());
    }

    private long versions(UUID playbookId) {
        return admin.sql("SELECT count(*) FROM playbook_versions WHERE playbook_id = ?").param(playbookId).query(Long.class).single();
    }

    private JsonNode versionContent(UUID playbookId, int number) {
        return json.readTree(admin.sql("SELECT content::text FROM playbook_versions WHERE playbook_id = ? AND number = ?")
                .params(playbookId, number).query(String.class).single());
    }

    private static String empty() {
        return "{\"schemaVersion\":1,\"stages\":[],\"discovery\":[],\"objections\":[],\"priceTable\":{\"extraColumnName\":null,"
                + "\"maxDiscount\":null,\"maxDiscountReview\":null,\"plans\":[]}}";
    }

    private static String items(int count, java.util.function.IntFunction<String> item) {
        return String.join(",", java.util.stream.IntStream.range(0, count).mapToObj(item).toList());
    }

    private static String stages(int count) {
        var items = new java.util.ArrayList<String>();
        for (int i = 0; i < count; i++) {
            items.add("{\"id\":\"" + UUID.randomUUID() + "\",\"name\":\"Etapa " + i + "\"}");
        }
        return String.join(",", items);
    }
}
