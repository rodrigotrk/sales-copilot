package app.salescopilot.auth;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

import app.salescopilot.auth.domain.EmailAddresses;
import app.salescopilot.shared.Problems;
import tools.jackson.databind.json.JsonMapper;

/** No máximo um pedido de link por e-mail a cada 60 segundos (AD-039). Roda antes do filtro que gera o token. */
class LoginLinkRequestLimiter extends OncePerRequestFilter {

    static final Duration INTERVAL = Duration.ofSeconds(60);

    private final JdbcClient jdbc;
    private final TransactionTemplate transactions;
    private final Clock clock;
    private final JsonMapper json;

    LoginLinkRequestLimiter(JdbcClient jdbc, TransactionTemplate transactions, Clock clock, JsonMapper json) {
        this.jdbc = jdbc;
        this.transactions = transactions;
        this.clock = clock;
        this.json = json;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !("POST".equals(request.getMethod()) && "/ott/generate".equals(request.getRequestURI().substring(request.getContextPath().length())));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String email = EmailAddresses.normalize(request.getParameter("username"));
        if (!EmailAddresses.looksValid(email)) {
            Problems.write(response, json, HttpStatus.UNPROCESSABLE_CONTENT, "invalid_email", "Informe um e-mail válido.",
                    request.getRequestURI());
            return;
        }
        Instant now = clock.instant();
        Boolean allowed = transactions.execute(status -> {
            // Trava a linha do e-mail para que dois pedidos simultâneos não passem juntos.
            jdbc.sql("INSERT INTO login_link_requests (email, requested_at) VALUES (?, ?) ON CONFLICT (email) DO NOTHING")
                    .params(email, Timestamp.from(Instant.EPOCH))
                    .update();
            Instant last = jdbc.sql("SELECT requested_at FROM login_link_requests WHERE email = ? FOR UPDATE")
                    .param(email)
                    .query(Timestamp.class)
                    .single()
                    .toInstant();
            if (Duration.between(last, now).compareTo(INTERVAL) < 0) {
                return false;
            }
            jdbc.sql("UPDATE login_link_requests SET requested_at = ? WHERE email = ?")
                    .params(Timestamp.from(now), email)
                    .update();
            return true;
        });
        if (!Boolean.TRUE.equals(allowed)) {
            Problems.write(response, json, HttpStatus.TOO_MANY_REQUESTS, "too_many_requests",
                    "Aguarde 1 minuto para pedir outro link.", request.getRequestURI());
            return;
        }
        chain.doFilter(request, response);
    }
}
