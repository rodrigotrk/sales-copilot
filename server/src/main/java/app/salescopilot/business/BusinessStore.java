package app.salescopilot.business;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** Consultas de negócio e associação; chamadas sempre dentro de {@code Tenancy}, que define as variáveis do RLS. */
@Component
class BusinessStore {

    record Membership(UUID businessId, String name, String role) {
    }

    private final JdbcClient jdbc;

    BusinessStore(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    List<Membership> membershipsOf(UUID userId) {
        return jdbc.sql("""
                SELECT b.id, b.name, m.role FROM memberships m JOIN businesses b ON b.id = m.business_id
                 WHERE m.user_id = ? ORDER BY b.name""")
                .param(userId)
                .query((rs, i) -> new Membership(rs.getObject("id", UUID.class), rs.getString("name"), rs.getString("role")))
                .list();
    }

    Optional<String> roleIn(UUID userId, UUID businessId) {
        return jdbc.sql("SELECT role FROM memberships WHERE user_id = ? AND business_id = ?")
                .params(userId, businessId)
                .query(String.class)
                .optional();
    }

    boolean isAllowedToSignUp(String email) {
        return jdbc.sql("SELECT count(*) FROM allowed_signups WHERE email = ?").param(email).query(Long.class).single() > 0;
    }

    /** Trava a linha da pessoa para serializar criações simultâneas dela (AD-050). */
    void lockUser(UUID userId) {
        jdbc.sql("SELECT id FROM users WHERE id = ? FOR UPDATE").param(userId).query(UUID.class).single();
    }

    boolean hasAnyMembership(UUID userId) {
        return jdbc.sql("SELECT count(*) FROM memberships WHERE user_id = ?").param(userId).query(Long.class).single() > 0;
    }

    void create(UUID businessId, String name, UUID gestorId) {
        jdbc.sql("INSERT INTO businesses (id, name) VALUES (?, ?)").params(businessId, name).update();
        jdbc.sql("INSERT INTO memberships (user_id, business_id, role) VALUES (?, ?, 'gestor')")
                .params(gestorId, businessId)
                .update();
    }
}
