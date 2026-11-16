package app.salescopilot.playbook;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import app.salescopilot.playbook.api.PlaybookDocument;
import tools.jackson.databind.json.JsonMapper;

/**
 * Tabelas `playbooks` e `playbook_versions`. Sempre chamado dentro da Tenancy: o RLS já restringe
 * tudo ao negócio da transação. Público só para {@code PlaybookDrafts}, no api do módulo.
 */
@Component
public class PlaybookStore {

    public record Row(UUID id, UUID businessId, String name, PlaybookDocument draft, long revision) {
    }

    record Version(int number, Instant publishedAt, PlaybookDocument content) {
    }

    private final JdbcClient jdbc;
    private final JsonMapper json;

    PlaybookStore(JdbcClient jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    void create(UUID businessId, String name) {
        jdbc.sql("INSERT INTO playbooks (id, business_id, name, draft, draft_revision) VALUES (?, ?, ?, ?::jsonb, 0)")
                .params(UUID.randomUUID(), businessId, name, json.writeValueAsString(PlaybookDocument.empty()))
                .update();
    }

    Optional<Row> current() {
        return jdbc.sql("SELECT id, business_id, name, draft, draft_revision FROM playbooks")
                .query((rs, i) -> row(rs))
                .optional();
    }

    public Optional<Row> lock(UUID playbookId) {
        return jdbc.sql("SELECT id, business_id, name, draft, draft_revision FROM playbooks WHERE id = ? FOR UPDATE")
                .param(playbookId)
                .query((rs, i) -> row(rs))
                .optional();
    }

    /** Grava o rascunho e devolve a revisão nova. */
    public long updateDraft(UUID playbookId, PlaybookDocument draft) {
        return jdbc.sql("""
                UPDATE playbooks SET draft = ?::jsonb, draft_revision = draft_revision + 1, updated_at = now()
                 WHERE id = ? RETURNING draft_revision""")
                .params(json.writeValueAsString(draft), playbookId)
                .query(Long.class)
                .single();
    }

    Optional<Version> latestVersion(UUID playbookId) {
        return jdbc.sql("""
                SELECT number, published_at, content FROM playbook_versions
                 WHERE playbook_id = ? ORDER BY number DESC LIMIT 1""")
                .param(playbookId)
                .query((rs, i) -> new Version(rs.getInt("number"), rs.getTimestamp("published_at").toInstant(),
                        json.readValue(rs.getString("content"), PlaybookDocument.class)))
                .optional();
    }

    void insertVersion(Row playbook, PlaybookDocument content, int number, UUID publishedBy) {
        jdbc.sql("""
                INSERT INTO playbook_versions (id, playbook_id, business_id, number, content, published_by)
                VALUES (?, ?, ?, ?, ?::jsonb, ?)""")
                .params(UUID.randomUUID(), playbook.id(), playbook.businessId(), number,
                        json.writeValueAsString(content), publishedBy)
                .update();
    }

    private Row row(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new Row(rs.getObject("id", UUID.class), rs.getObject("business_id", UUID.class), rs.getString("name"),
                json.readValue(rs.getString("draft"), PlaybookDocument.class), rs.getLong("draft_revision"));
    }
}
