package app.salescopilot.auth;

import java.time.Clock;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

import app.salescopilot.auth.api.CurrentUser;

/** Grava na sessão quem entrou e quando; os atributos são tipos simples porque a sessão vai ao Postgres. */
@Component
class SignedInSession {

    static final String LOGIN_AT = "auth.loginAt";

    private final Clock clock;
    private final SecurityContextRepository contexts = new HttpSessionSecurityContextRepository();

    SignedInSession(Clock clock) {
        this.clock = clock;
    }

    /** Depois de um login que o próprio Spring Security autenticou (link ou Google). */
    void remember(HttpServletRequest request, CurrentUser user) {
        HttpSession session = request.getSession(true);
        session.setAttribute(CurrentUser.SESSION_USER_ID, user.id().toString());
        session.setAttribute(CurrentUser.SESSION_EMAIL, user.email());
        session.setAttribute(LOGIN_AT, clock.millis());
    }

    /** Para o login pela confirmação de associação, que não passa por um filtro do Spring Security. */
    void signIn(HttpServletRequest request, HttpServletResponse response, CurrentUser user) {
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(user.email(), null,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        contexts.saveContext(context, request, response);
        remember(request, user);
    }

    void signOut(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }
}
