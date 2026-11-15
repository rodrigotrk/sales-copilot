package app.salescopilot.auth;

import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import app.salescopilot.auth.api.CurrentUser;

/** Tabela `users`, sem RLS: só o login a usa (AD-037). */
@Component
class UserAccounts {

    private final JdbcClient jdbc;

    UserAccounts(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    CurrentUser findOrCreateByEmail(String email) {
        jdbc.sql("INSERT INTO users (id, email) VALUES (?, ?) ON CONFLICT (email) DO NOTHING")
                .params(UUID.randomUUID(), email)
                .update();
        return jdbc.sql("SELECT id, email FROM users WHERE email = ?")
                .param(email)
                .query((rs, i) -> new CurrentUser(rs.getObject("id", UUID.class), rs.getString("email")))
                .single();
    }

    Optional<CurrentUser> findByGoogleSubject(String subject) {
        return jdbc.sql("SELECT id, email FROM users WHERE google_subject = ?")
                .param(subject)
                .query((rs, i) -> new CurrentUser(rs.getObject("id", UUID.class), rs.getString("email")))
                .optional();
    }

    void linkGoogle(UUID userId, String subject) {
        jdbc.sql("UPDATE users SET google_subject = ? WHERE id = ?").params(subject, userId).update();
    }
}
