package app.salescopilot.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Desafio de associação da AD-052: liga um `google_subject` a um e-mail só quando alguém usa o link
 * enviado para aquela tentativa. O banco guarda só o hash do token.
 */
@Component
class IdentityLinkChallenges {

    static final Duration VALIDITY = Duration.ofMinutes(15);

    record Pending(String email, String googleSubject) {
    }

    private final JdbcClient jdbc;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    IdentityLinkChallenges(JdbcClient jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    /** Devolve o token que vai no link. */
    String create(String email, String googleSubject) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        jdbc.sql("""
                INSERT INTO identity_link_challenges (id, token_hash, email, google_subject, expires_at)
                VALUES (?, ?, ?, ?, ?)""")
                .params(UUID.randomUUID(), hash(token), email, googleSubject,
                        Timestamp.from(clock.instant().plus(VALIDITY)))
                .update();
        return token;
    }

    Optional<String> pendingEmail(String token) {
        return jdbc.sql("""
                SELECT email FROM identity_link_challenges
                 WHERE token_hash = ? AND consumed_at IS NULL AND expires_at > ?""")
                .params(hash(token), Timestamp.from(clock.instant()))
                .query(String.class)
                .optional();
    }

    /** Consome o desafio numa única instrução: entre dois consumos simultâneos, só um recebe a linha. */
    Optional<Pending> consume(String token) {
        Instant now = clock.instant();
        return jdbc.sql("""
                UPDATE identity_link_challenges SET consumed_at = ?
                 WHERE token_hash = ? AND consumed_at IS NULL AND expires_at > ?
                RETURNING email, google_subject""")
                .params(Timestamp.from(now), hash(token), Timestamp.from(now))
                .query((rs, i) -> new Pending(rs.getString("email"), rs.getString("google_subject")))
                .optional();
    }

    private static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível na JVM", e);
        }
    }
}
