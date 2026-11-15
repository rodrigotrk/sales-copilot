package app.salescopilot.auth.api;

import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.springframework.http.HttpStatus;

import app.salescopilot.shared.ApiException;

/** Usuário da sessão. Um controller recebe este parâmetro só quando há login. */
public record CurrentUser(UUID id, String email) {

    public static final String SESSION_USER_ID = "auth.userId";
    public static final String SESSION_EMAIL = "auth.email";

    public static CurrentUser require(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Object id = session == null ? null : session.getAttribute(SESSION_USER_ID);
        if (id == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "unauthenticated", "Entre para continuar.");
        }
        return new CurrentUser(UUID.fromString((String) id), (String) session.getAttribute(SESSION_EMAIL));
    }
}
