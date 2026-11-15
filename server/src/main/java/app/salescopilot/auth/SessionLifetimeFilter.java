package app.salescopilot.auth;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Prazo absoluto de 30 dias desde o login (AD-039). O prazo de 7 dias sem uso é o tempo de
 * inatividade da própria sessão (`spring.session.timeout`).
 */
class SessionLifetimeFilter extends OncePerRequestFilter {

    static final Duration ABSOLUTE = Duration.ofDays(30);

    private final Clock clock;

    SessionLifetimeFilter(Clock clock) {
        this.clock = clock;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute(SignedInSession.LOGIN_AT) instanceof Long loginAt
                && clock.millis() - loginAt > ABSOLUTE.toMillis()) {
            session.invalidate();
        }
        chain.doFilter(request, response);
    }
}
