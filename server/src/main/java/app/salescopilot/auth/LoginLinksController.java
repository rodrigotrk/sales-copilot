package app.salescopilot.auth;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import app.salescopilot.auth.api.CurrentUser;
import app.salescopilot.shared.ApiException;

/** Leitura dos links sem consumi-los (AD-048) e consumo do desafio de associação com o Google (AD-052). */
@RestController
class LoginLinksController {

    private final JdbcClient jdbc;
    private final Clock clock;
    private final IdentityLinkChallenges challenges;
    private final UserAccounts users;
    private final SignedInSession session;
    private final TransactionTemplate transactions;

    LoginLinksController(JdbcClient jdbc, Clock clock, IdentityLinkChallenges challenges, UserAccounts users,
            SignedInSession session, TransactionTemplate transactions) {
        this.jdbc = jdbc;
        this.clock = clock;
        this.challenges = challenges;
        this.users = users;
        this.session = session;
        this.transactions = transactions;
    }

    @GetMapping("/api/login-links/{token}")
    Map<String, String> loginLink(@PathVariable String token) {
        String email = jdbc.sql("SELECT username FROM one_time_tokens WHERE token_value = ? AND expires_at > ?")
                .params(token, Timestamp.from(clock.instant()))
                .query(String.class)
                .optional()
                .orElseThrow(LoginLinksController::invalidToken);
        return Map.of("email", email);
    }

    @GetMapping("/api/identity-links/{token}")
    Map<String, String> identityLink(@PathVariable String token) {
        return Map.of("email", challenges.pendingEmail(token).orElseThrow(LoginLinksController::invalidToken));
    }

    @PostMapping("/login/identity-link")
    void consumeIdentityLink(@RequestParam String token, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        CurrentUser user = transactions.execute(status -> challenges.consume(token)
                .map(pending -> {
                    CurrentUser found = users.findOrCreateByEmail(pending.email());
                    users.linkGoogle(found.id(), pending.googleSubject());
                    return found;
                })
                .orElse(null));
        if (user == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "invalid_token",
                    "Este link já foi usado ou expirou. Peça um novo.");
        }
        session.signIn(request, response, user);
        response.sendRedirect("/");
    }

    private static ApiException invalidToken() {
        return new ApiException(HttpStatus.NOT_FOUND, "invalid_token", "Este link já foi usado ou expirou. Peça um novo.");
    }
}
