package app.salescopilot.support;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** Base dos testes que sobem o servidor inteiro sobre o Postgres de {@link TestDatabase}. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(IntegrationTest.Beans.class)
public abstract class IntegrationTest {

    protected static final StubServer STUB = StubServer.start();
    protected static final Path IMPORT_DIRECTORY;

    static {
        try {
            IMPORT_DIRECTORY = Files.createTempDirectory("imports");
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", TestDatabase.POSTGRES::getJdbcUrl);
        registry.add("spring.ai.openai.base-url", () -> STUB.url() + "/v1");
        registry.add("app.import.google-docs-base-url", STUB::url);
        registry.add("app.import.directory", IMPORT_DIRECTORY::toString);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Beans {

        @Bean
        @Primary
        MutableClock testClock() {
            return new MutableClock();
        }

        @Bean
        @Primary
        Mailbox testMailbox() {
            return new Mailbox();
        }
    }

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected MutableClock clock;

    @Autowired
    protected Mailbox mailbox;

    @Autowired
    protected JsonMapper json;

    protected final JdbcClient admin = TestDatabase.admin();

    @BeforeEach
    void resetCollaborators() {
        clock.set(Instant.now());
        mailbox.clear();
        STUB.reset();
    }

    protected static String uniqueEmail() {
        return "pessoa-" + UUID.randomUUID().toString().substring(0, 8) + "@exemplo.test";
    }

    protected void allowSignup(String email) {
        admin.sql("INSERT INTO allowed_signups (email) VALUES (?) ON CONFLICT DO NOTHING").param(email).update();
    }

    /** Pede o link, abre e usa "Entrar": devolve o cookie da sessão criada. */
    protected Cookie login(String email) throws Exception {
        clock.advance(java.time.Duration.ofMinutes(2));
        mvc.perform(post("/ott/generate").param("username", email).with(csrf()));
        String token = mailbox.lastToken(email, "/entrar/link");
        MvcResult result = mvc.perform(post("/login/ott").param("token", token).with(csrf())).andReturn();
        Cookie session = result.getResponse().getCookie("SESSION");
        if (session == null) {
            throw new AssertionError("login sem sessão: " + result.getResponse().getStatus());
        }
        return session;
    }

    /** Pessoa liberada que cria um negócio: devolve o cookie e o id do negócio. */
    protected Gestor newGestor() throws Exception {
        String email = uniqueEmail();
        allowSignup(email);
        Cookie session = login(email);
        String body = mvc.perform(post("/api/businesses").cookie(session).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"businessName\":\"Energia Solar Exemplo\",\"playbookName\":\"Venda consultiva\"}"))
                .andReturn().getResponse().getContentAsString();
        UUID businessId = UUID.fromString(json.readTree(body).get("businessId").asString());
        return new Gestor(email, session, businessId);
    }

    protected JsonNode getPlaybook(Gestor gestor) throws Exception {
        return json.readTree(mvc.perform(get("/api/businesses/" + gestor.businessId() + "/playbook").cookie(gestor.session()))
                .andReturn().getResponse().getContentAsString());
    }

    protected UUID userId(String email) {
        return admin.sql("SELECT id FROM users WHERE email = ?").param(email).query(UUID.class).single();
    }

    protected Map<String, Object> jobRow(UUID jobId) {
        return admin.sql("SELECT * FROM import_jobs WHERE id = ?").param(jobId).query().singleRow();
    }

    protected List<UUID> jobsOf(UUID businessId) {
        return admin.sql("SELECT id FROM import_jobs WHERE business_id = ?").param(businessId).query(UUID.class).list();
    }

    public record Gestor(String email, Cookie session, UUID businessId) {
    }
}
