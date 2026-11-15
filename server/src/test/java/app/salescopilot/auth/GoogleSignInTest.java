package app.salescopilot.auth;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;

import app.salescopilot.support.IntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GoogleSignInTest extends IntegrationTest {

    @Autowired
    GoogleSignInHandler handler;

    @Autowired
    IdentityLinkChallenges challenges;

    @Test
    @DisplayName("[C07a] Google sem e-mail verificado não cria sessão")
    void unverifiedEmailIsRejected() throws Exception {
        String email = uniqueEmail().replace("@exemplo.test", "@gmail.com");

        var result = googleReturns("sub-" + email, email, false, null);

        assertThat(result.response().getRedirectedUrl()).isEqualTo("/entrar?erro=google_unverified");
        assertThat(result.request().getSession(false)).isNull();
        assertThat(admin.sql("SELECT count(*) FROM users WHERE email = ?").param(email).query(Long.class).single()).isZero();
    }

    @Test
    @DisplayName("[C08] Google com gmail ou hd entra e liga a identidade pelo e-mail")
    void authoritativeGoogleSignsInAndLinksByEmail() throws Exception {
        String gmail = uniqueEmail().replace("@exemplo.test", "@gmail.com");
        login(gmail);
        var gmailResult = googleReturns("sub-gmail-" + gmail, gmail, true, null);
        assertThat(gmailResult.response().getRedirectedUrl()).isEqualTo("/");
        assertThat(gmailResult.request().getSession(false).getAttribute("auth.userId")).isEqualTo(userId(gmail).toString());
        assertThat(googleSubjectOf(gmail)).contains("sub-gmail-" + gmail);

        String workspace = uniqueEmail().replace("@exemplo.test", "@empresa.com.br");
        var workspaceResult = googleReturns("sub-ws-" + workspace, workspace, true, "empresa.com.br");
        assertThat(workspaceResult.response().getRedirectedUrl()).isEqualTo("/");
        assertThat(googleSubjectOf(workspace)).contains("sub-ws-" + workspace);

        var again = googleReturns("sub-ws-" + workspace, "outro-" + workspace, true, "empresa.com.br");
        assertThat(again.request().getSession(false).getAttribute("auth.userId")).isEqualTo(userId(workspace).toString());
        assertThat(mvc.perform(get("/oauth2/authorization/google")).andReturn().getResponse().getStatus()).isEqualTo(302);
    }

    @Test
    @DisplayName("[C09a] Google fora do Gmail sem hd grava desafio, envia link e não cria sessão")
    void nonAuthoritativeGoogleCreatesChallenge() throws Exception {
        String email = uniqueEmail().replace("@exemplo.test", "@empresa.com.br");
        login(email);

        var result = googleReturns("sub-" + email, email, true, null);

        var redirect = org.springframework.web.util.UriComponentsBuilder.fromUriString(result.response().getRedirectedUrl()).build();
        assertThat(redirect.getPath()).isEqualTo("/entrar");
        assertThat(java.net.URLDecoder.decode(redirect.getQueryParams().getFirst("confirmar"), java.nio.charset.StandardCharsets.UTF_8))
                .isEqualTo(email);
        assertThat(result.request().getSession(false)).isNull();
        assertThat(googleSubjectOf(email)).isEmpty();
        assertThat(admin.sql("SELECT google_subject FROM identity_link_challenges WHERE email = ?").param(email)
                .query(String.class).single()).isEqualTo("sub-" + email);
        assertThat(mailbox.lastToken(email, "/entrar/vincular")).isNotBlank();
    }

    @Test
    @DisplayName("[C10a] consumir o desafio grava google_subject e cria a sessão")
    void consumingTheChallengeLinksAndSignsIn() throws Exception {
        String email = uniqueEmail().replace("@exemplo.test", "@empresa.com.br");
        googleReturns("sub-" + email, email, true, null);
        String token = mailbox.lastToken(email, "/entrar/vincular");
        mvc.perform(get("/api/identity-links/" + token)).andExpect(status().isOk()).andExpect(jsonPath("$.email").value(email));

        mvc.perform(post("/login/identity-link").param("token", token)).andExpect(status().isForbidden());
        var response = mvc.perform(post("/login/identity-link").param("token", token).with(csrf()))
                .andExpect(status().isFound())
                .andReturn().getResponse();
        Cookie session = response.getCookie("SESSION");

        assertThat(googleSubjectOf(email)).contains("sub-" + email);
        mvc.perform(get("/api/me").cookie(session)).andExpect(status().isOk()).andExpect(jsonPath("$.user.email").value(email));
        var next = googleReturns("sub-" + email, email, true, null);
        assertThat(next.response().getRedirectedUrl()).isEqualTo("/");
    }

    @Test
    @DisplayName("[C10b] dois consumos simultâneos do desafio têm um só vencedor")
    void concurrentConsumesHaveOneWinner() throws Exception {
        String email = uniqueEmail().replace("@exemplo.test", "@empresa.com.br");
        String token = challenges.create(email, "sub-" + email);
        var start = new CountDownLatch(1);
        Callable<Optional<IdentityLinkChallenges.Pending>> consume = () -> {
            start.await();
            return challenges.consume(token);
        };
        try (var pool = Executors.newFixedThreadPool(2)) {
            var first = pool.submit(consume);
            var second = pool.submit(consume);
            start.countDown();
            List<Boolean> won = new ArrayList<>(List.of(first.get().isPresent(), second.get().isPresent()));
            assertThat(won).containsExactlyInAnyOrder(true, false);
        }
    }

    @Test
    @DisplayName("[C11] link comum não liga identidade pendente e desafio vencido é recusado")
    void ordinaryLinkDoesNotLinkAndExpiredChallengeIsRefused() throws Exception {
        String email = uniqueEmail().replace("@exemplo.test", "@empresa.com.br");
        googleReturns("sub-" + email, email, true, null);
        String challenge = mailbox.lastToken(email, "/entrar/vincular");

        Cookie session = login(email);
        mvc.perform(get("/api/me").cookie(session)).andExpect(status().isOk());
        assertThat(googleSubjectOf(email)).isEmpty();
        clock.advance(Duration.ofMinutes(-2));
        mvc.perform(get("/api/identity-links/" + challenge)).andExpect(status().isOk());

        clock.advance(Duration.ofMinutes(16));
        mvc.perform(get("/api/identity-links/" + challenge)).andExpect(status().isNotFound());
        mvc.perform(post("/login/identity-link").param("token", challenge).with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("invalid_token"));
        assertThat(googleSubjectOf(email)).isEmpty();
    }

    private record Returned(MockHttpServletRequest request, MockHttpServletResponse response) {
    }

    /** O retorno do Google como o Spring Security o entrega ao handler depois de validar o id token. */
    private Returned googleReturns(String subject, String email, boolean verified, String hostedDomain) throws Exception {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", subject);
        claims.put("email", email);
        claims.put("email_verified", verified);
        if (hostedDomain != null) {
            claims.put("hd", hostedDomain);
        }
        var token = new OidcIdToken("id-token", Instant.now(), Instant.now().plusSeconds(60), claims);
        var user = new DefaultOidcUser(List.of(new SimpleGrantedAuthority("OIDC_USER")), token);
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        request.getSession(true);
        handler.onAuthenticationSuccess(request, response, new TestingAuthenticationToken(user, null, "OIDC_USER"));
        return new Returned(request, response);
    }

    private Optional<String> googleSubjectOf(String email) {
        return admin.sql("SELECT google_subject FROM users WHERE email = ?").param(email).query(String.class).optional()
                .filter(java.util.Objects::nonNull);
    }
}
