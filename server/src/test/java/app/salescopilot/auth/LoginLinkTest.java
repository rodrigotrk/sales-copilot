package app.salescopilot.auth;

import java.time.Duration;
import java.util.Base64;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.transaction.support.TransactionTemplate;

import app.salescopilot.support.IntegrationTest;
import app.salescopilot.support.TestDatabase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LoginLinkTest extends IntegrationTest {

    @Test
    @DisplayName("[C02a] pedir o link envia o e-mail e responde igual para e-mail desconhecido")
    void sendsLinkAndAnswersTheSameForUnknownEmail() throws Exception {
        String known = uniqueEmail();
        login(known);
        String unknown = uniqueEmail();
        clock.advance(Duration.ofMinutes(2));

        var forKnown = mvc.perform(post("/ott/generate").param("username", known).with(csrf())).andReturn().getResponse();
        var forUnknown = mvc.perform(post("/ott/generate").param("username", unknown).with(csrf())).andReturn().getResponse();

        assertThat(forKnown.getStatus()).isEqualTo(200);
        assertThat(forUnknown.getStatus()).isEqualTo(200);
        assertThat(forUnknown.getContentAsString()).isEqualTo(forKnown.getContentAsString());
        assertThat(mailbox.to(unknown)).hasSize(1);
        assertThat(mailbox.to(unknown).getFirst().getText()).contains("http://localhost:5173/entrar/link?token=");
        mvc.perform(post("/ott/generate").param("username", uniqueEmail())).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("[C03a] segundo pedido em menos de 60 s responde 429 e não envia")
    void secondRequestWithinSixtySecondsIsRefused() throws Exception {
        String email = uniqueEmail();
        mvc.perform(post("/ott/generate").param("username", email).with(csrf())).andExpect(status().isOk());
        clock.advance(Duration.ofSeconds(59));

        mvc.perform(post("/ott/generate").param("username", email).with(csrf()))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Content-Type", "application/problem+json"))
                .andExpect(jsonPath("$.code").value("too_many_requests"));
        assertThat(mailbox.to(email)).hasSize(1);

        clock.advance(Duration.ofSeconds(2));
        mvc.perform(post("/ott/generate").param("username", email).with(csrf())).andExpect(status().isOk());
        assertThat(mailbox.to(email)).hasSize(2);
    }

    @Test
    @DisplayName("[C04a] ler o link duas vezes não consome o token")
    void readingTheLinkDoesNotConsumeIt() throws Exception {
        String email = uniqueEmail();
        mvc.perform(post("/ott/generate").param("username", email).with(csrf()));
        String token = mailbox.lastToken(email, "/entrar/link");

        mvc.perform(get("/api/login-links/" + token)).andExpect(status().isOk()).andExpect(jsonPath("$.email").value(email));
        mvc.perform(get("/api/login-links/" + token)).andExpect(status().isOk()).andExpect(jsonPath("$.email").value(email));
        mvc.perform(post("/login/ott").param("token", token).with(csrf())).andExpect(status().isFound());
    }

    @Test
    @DisplayName("[C05] POST /login/ott consome o token e cria a sessão")
    void postingTheTokenCreatesTheSession() throws Exception {
        String email = uniqueEmail();
        mvc.perform(post("/ott/generate").param("username", email).with(csrf()));
        String token = mailbox.lastToken(email, "/entrar/link");

        mvc.perform(post("/login/ott").param("token", token)).andExpect(status().isForbidden());
        var response = mvc.perform(post("/login/ott").param("token", token).with(csrf()))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "/"))
                .andReturn().getResponse();
        Cookie session = response.getCookie("SESSION");

        mvc.perform(get("/api/me").cookie(session)).andExpect(status().isOk()).andExpect(jsonPath("$.user.email").value(email));
        mvc.perform(get("/api/login-links/" + token)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("[C06a] token aos 14 min entra, reuso e 16 min são recusados")
    void tokenExpiresAfterFifteenMinutesAndIsSingleUse() throws Exception {
        String email = uniqueEmail();
        mvc.perform(post("/ott/generate").param("username", email).with(csrf()));
        String first = mailbox.lastToken(email, "/entrar/link");
        clock.advance(Duration.ofMinutes(14));
        mvc.perform(post("/login/ott").param("token", first).with(csrf())).andExpect(status().isFound());

        mvc.perform(post("/login/ott").param("token", first).with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("invalid_token"));

        clock.advance(Duration.ofMinutes(1));
        mvc.perform(post("/ott/generate").param("username", email).with(csrf()));
        String second = mailbox.lastToken(email, "/entrar/link");
        clock.advance(Duration.ofMinutes(16));
        mvc.perform(get("/api/login-links/" + second)).andExpect(status().isNotFound());
        mvc.perform(post("/login/ott").param("token", second).with(csrf())).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("[C24a] Sair invalida a sessão e o cookie antigo recebe 401")
    void logoutInvalidatesTheSession() throws Exception {
        Cookie session = login(uniqueEmail());
        mvc.perform(post("/logout").cookie(session)).andExpect(status().isForbidden());

        mvc.perform(post("/logout").cookie(session).with(csrf())).andExpect(status().isNoContent());

        mvc.perform(get("/api/me").cookie(session))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Content-Type", "application/problem+json"));
        mvc.perform(post("/logout").with(csrf())).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("[C25] sessão expira com 7 dias sem uso ou 30 dias desde o login")
    void sessionExpiresAfterIdleOrAbsoluteLimit() throws Exception {
        Cookie idle = login(uniqueEmail());
        String idleId = new String(Base64.getDecoder().decode(idle.getValue()));
        assertThat(TestDatabase.admin().sql("SELECT max_inactive_interval FROM spring_session WHERE session_id = ?")
                .param(idleId).query(Integer.class).single()).isEqualTo((int) Duration.ofDays(7).toSeconds());
        long sevenDaysAndOneSecondAgo = System.currentTimeMillis() - Duration.ofDays(7).plusSeconds(1).toMillis();
        TestDatabase.admin().sql("UPDATE spring_session SET last_access_time = ?, expiry_time = ? WHERE session_id = ?")
                .params(sevenDaysAndOneSecondAgo, sevenDaysAndOneSecondAgo + Duration.ofDays(7).toMillis(), idleId)
                .update();
        mvc.perform(get("/api/me").cookie(idle)).andExpect(status().isUnauthorized());

        Cookie active = login(uniqueEmail());
        clock.advance(Duration.ofDays(29));
        mvc.perform(get("/api/me").cookie(active)).andExpect(status().isOk());
        clock.advance(Duration.ofDays(1).plusSeconds(1));
        mvc.perform(get("/api/me").cookie(active)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("[C26] sessão gravada no Postgres vale para outra instância do repositório")
    void sessionLivesInPostgres() throws Exception {
        String email = uniqueEmail();
        Cookie session = login(email);
        String sessionId = new String(Base64.getDecoder().decode(session.getValue()));

        var dataSource = TestDatabase.appRole();
        var otherServer = new JdbcIndexedSessionRepository(new org.springframework.jdbc.core.JdbcTemplate(dataSource),
                new TransactionTemplate(new DataSourceTransactionManager(dataSource)));
        org.springframework.session.Session restored = otherServer.findById(sessionId);

        assertThat(restored).isNotNull();
        assertThat((String) restored.getAttribute("auth.email")).isEqualTo(email);
        assertThat(TestDatabase.admin().sql("SELECT principal_name FROM spring_session WHERE session_id = ?")
                .param(sessionId).query(String.class).single()).isEqualTo(email);
    }
}
