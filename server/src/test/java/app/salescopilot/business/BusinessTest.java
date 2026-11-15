package app.salescopilot.business;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import app.salescopilot.support.Drafts;
import app.salescopilot.support.IntegrationTest;
import app.salescopilot.support.TestDatabase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BusinessTest extends IntegrationTest {

    private static final String NAMES = "{\"businessName\":\"Energia Solar Exemplo\",\"playbookName\":\"Venda consultiva\"}";

    @Test
    @DisplayName("[C12a] criar negócio liberado cria gestor e playbook e valida 1 a 80 caracteres")
    void allowedPersonCreatesBusiness() throws Exception {
        String email = uniqueEmail();
        allowSignup(email);
        Cookie session = login(email);
        mvc.perform(post("/api/businesses").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(NAMES))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/businesses").cookie(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"businessName\":\"" + "x".repeat(81) + "\",\"playbookName\":\" \"}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors[0].field").value("businessName"))
                .andExpect(jsonPath("$.errors[1].field").value("playbookName"));

        String body = mvc.perform(post("/api/businesses").cookie(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"businessName\":\"" + "x".repeat(80) + "\",\"playbookName\":\"P\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID businessId = UUID.fromString(json.readTree(body).get("businessId").asString());
        assertThat(admin.sql("SELECT role FROM memberships WHERE business_id = ? AND user_id = ?")
                .params(businessId, userId(email)).query(String.class).single()).isEqualTo("gestor");
        assertThat(admin.sql("SELECT name FROM playbooks WHERE business_id = ?").param(businessId).query(String.class).single())
                .isEqualTo("P");
    }

    @Test
    @DisplayName("[C13a] e-mail fora da lista responde 403 signup_not_allowed")
    void personOutsideTheListCannotCreate() throws Exception {
        String email = uniqueEmail();
        Cookie session = login(email);
        mvc.perform(get("/api/me").cookie(session)).andExpect(jsonPath("$.canCreateBusiness").value(false));
        mvc.perform(post("/api/businesses").cookie(session).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(NAMES))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("signup_not_allowed"));
        assertThat(admin.sql("SELECT count(*) FROM memberships WHERE user_id = ?").param(userId(email))
                .query(Long.class).single()).isZero();
    }

    @Test
    @DisplayName("[C14] membro que cria negócio recebe 409 already_member")
    void memberCannotCreateAnother() throws Exception {
        Gestor gestor = newGestor();
        mvc.perform(post("/api/businesses").cookie(gestor.session()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(NAMES))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("already_member"));
        assertThat(admin.sql("SELECT count(*) FROM memberships WHERE user_id = ?").param(userId(gestor.email()))
                .query(Long.class).single()).isEqualTo(1);
    }

    @Test
    @DisplayName("[C15] duas criações simultâneas criam um negócio e um 409")
    void concurrentCreationsMakeOneBusiness() throws Exception {
        String email = uniqueEmail();
        allowSignup(email);
        Cookie session = login(email);
        var start = new CountDownLatch(1);
        Callable<Integer> create = () -> {
            start.await();
            return mvc.perform(post("/api/businesses").cookie(session).with(csrf())
                    .contentType(MediaType.APPLICATION_JSON).content(NAMES)).andReturn().getResponse().getStatus();
        };
        try (var pool = Executors.newFixedThreadPool(2)) {
            var first = pool.submit(create);
            var second = pool.submit(create);
            start.countDown();
            assertThat(List.of(first.get(), second.get())).containsExactlyInAnyOrder(201, 409);
        }
        assertThat(admin.sql("SELECT count(*) FROM memberships WHERE user_id = ?").param(userId(email))
                .query(Long.class).single()).isEqualTo(1);
    }

    @Test
    @DisplayName("[C16a] /api/me devolve o último negócio usado")
    void meReturnsTheLastBusinessUsed() throws Exception {
        Gestor gestor = newGestor();
        UUID other = addSecondBusiness(gestor.email());
        mvc.perform(get("/api/businesses/" + other + "/playbook").cookie(gestor.session())).andExpect(status().isOk());

        mvc.perform(get("/api/me").cookie(gestor.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.businesses.length()").value(2))
                .andExpect(jsonPath("$.lastBusinessId").value(other.toString()));

        mvc.perform(get("/api/businesses/" + gestor.businessId() + "/playbook").cookie(gestor.session()));
        mvc.perform(get("/api/me").cookie(gestor.session()))
                .andExpect(jsonPath("$.lastBusinessId").value(gestor.businessId().toString()));
    }

    @Test
    @DisplayName("[C19] businessId sem associação ou playbookId de outro negócio responde 404")
    void businessFromTheAddressDecides() throws Exception {
        Gestor gestor = newGestor();
        UUID businessB = addSecondBusiness(gestor.email());
        Gestor stranger = newGestor();
        var playbookA = getPlaybook(gestor);
        UUID playbookIdA = UUID.fromString(playbookA.get("playbookId").asString());

        mvc.perform(get("/api/businesses/" + gestor.businessId() + "/playbook")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/businesses/" + gestor.businessId() + "/playbook").cookie(stranger.session()))
                .andExpect(status().isNotFound());
        mvc.perform(put("/api/businesses/" + businessB + "/playbook/draft").cookie(gestor.session()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(Drafts.saveBody(playbookIdA, Drafts.complete(), 0)))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/businesses/" + businessB + "/playbook/versions").cookie(gestor.session()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(Drafts.publishBody(playbookIdA, 0)))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/businesses/" + businessB + "/playbook/imports").cookie(gestor.session()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"playbookId\":\"" + playbookIdA + "\",\"text\":\"x\"}"))
                .andExpect(status().isNotFound());

        mvc.perform(get("/api/businesses/" + businessB + "/playbook").cookie(gestor.session())).andExpect(status().isOk());
        mvc.perform(put("/api/businesses/" + gestor.businessId() + "/playbook/draft").cookie(gestor.session()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(Drafts.saveBody(playbookIdA, Drafts.complete(), 0)))
                .andExpect(status().isOk());
        assertThat(admin.sql("SELECT draft_revision FROM playbooks WHERE id = ?").param(playbookIdA).query(Long.class).single())
                .isEqualTo(1);
        assertThat(admin.sql("SELECT draft_revision FROM playbooks WHERE business_id = ?").param(businessB).query(Long.class)
                .single()).isZero();
    }

    @Test
    @DisplayName("[C20] RLS do papel app devolve só o negócio da variável")
    void rowLevelSecurityFollowsTheVariable() throws Exception {
        Gestor a = newGestor();
        Gestor b = newGestor();
        var app = TestDatabase.appRole();
        var jdbc = JdbcClient.create(app);
        var transactions = new TransactionTemplate(new DataSourceTransactionManager(app));

        transactions.executeWithoutResult(status -> {
            jdbc.sql("SELECT set_config('app.business_id', ?, true)").param(a.businessId().toString()).query().singleValue();
            assertThat(jdbc.sql("SELECT business_id FROM playbooks").query(UUID.class).list()).containsExactly(a.businessId());
            assertThat(jdbc.sql("UPDATE playbooks SET name = 'invadido' WHERE business_id = ?").param(b.businessId()).update())
                    .isZero();
        });
        assertThatThrownBy(() -> transactions.executeWithoutResult(
                status -> jdbc.sql("SELECT count(*) FROM playbooks").query(Long.class).single()))
                .rootCause().hasMessageContaining("app.business_id");
    }

    @Test
    @DisplayName("[C21] chave composta recusa business_id divergente")
    void compositeKeyRefusesMismatchedBusiness() throws Exception {
        Gestor a = newGestor();
        Gestor b = newGestor();
        UUID playbookA = UUID.fromString(getPlaybook(a).get("playbookId").asString());
        var app = TestDatabase.appRole();
        var jdbc = JdbcClient.create(app);
        var transactions = new TransactionTemplate(new DataSourceTransactionManager(app));

        assertThatThrownBy(() -> transactions.executeWithoutResult(status -> {
            jdbc.sql("SELECT set_config('app.business_id', ?, true)").param(b.businessId().toString()).query().singleValue();
            jdbc.sql("INSERT INTO import_jobs (id, business_id, playbook_id, status, source_kind) VALUES (?, ?, ?, 'queued', 'text')")
                    .params(UUID.randomUUID(), b.businessId(), playbookA).update();
        })).hasRootCauseInstanceOf(SQLException.class).hasMessageContaining("foreign key");
    }

    @Test
    @DisplayName("[C22] closer recebe 403 nas escritas")
    void closerCannotWrite() throws Exception {
        Gestor gestor = newGestor();
        String closerEmail = uniqueEmail();
        Cookie closer = login(closerEmail);
        admin.sql("INSERT INTO memberships (user_id, business_id, role) VALUES (?, ?, 'closer')")
                .params(userId(closerEmail), gestor.businessId()).update();
        UUID playbookId = UUID.fromString(getPlaybook(gestor).get("playbookId").asString());
        String base = "/api/businesses/" + gestor.businessId() + "/playbook";

        mvc.perform(get(base).cookie(closer)).andExpect(status().isOk());
        mvc.perform(put(base + "/draft").cookie(closer).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(Drafts.saveBody(playbookId, Drafts.complete(), 0))).andExpect(status().isForbidden());
        mvc.perform(post(base + "/versions").cookie(closer).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(Drafts.publishBody(playbookId, 0))).andExpect(status().isForbidden());
        mvc.perform(multipart(base + "/imports").file("file", "texto".getBytes()).param("playbookId", playbookId.toString())
                .cookie(closer).with(csrf())).andExpect(status().isForbidden());
        assertThat(admin.sql("SELECT draft_revision FROM playbooks WHERE id = ?").param(playbookId).query(Long.class).single())
                .isZero();
    }

    private UUID addSecondBusiness(String email) {
        UUID businessId = UUID.randomUUID();
        admin.sql("INSERT INTO businesses (id, name) VALUES (?, 'Segundo negócio')").param(businessId).update();
        admin.sql("INSERT INTO memberships (user_id, business_id, role) VALUES (?, ?, 'gestor')")
                .params(userId(email), businessId).update();
        admin.sql("INSERT INTO playbooks (id, business_id, name, draft, draft_revision) VALUES (?, ?, 'Outro', "
                + "'{\"schemaVersion\":1,\"stages\":[],\"discovery\":[],\"objections\":[],\"priceTable\":{\"extraColumnName\":null,"
                + "\"maxDiscount\":null,\"maxDiscountReview\":null,\"plans\":[]}}'::jsonb, 0)")
                .params(UUID.randomUUID(), businessId).update();
        return businessId;
    }

}
