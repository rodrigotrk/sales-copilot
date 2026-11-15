package app.salescopilot.importing;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import app.salescopilot.playbook.api.ImportActivity.ImportView;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/** Tabela `import_jobs`. Fora de {@link #claim()} e {@link #statuses}, roda dentro da Tenancy, sob RLS. */
@Component
class ImportJobs {

    static final int LEASE_SECONDS = 600;

    record Job(UUID id, UUID businessId, UUID playbookId, String status, String sourceKind, String filePath,
            String sourceUrl) {
    }

    record Claim(UUID jobId, UUID businessId, UUID leaseToken) {
    }

    private final JdbcClient jdbc;
    private final JsonMapper json;

    ImportJobs(JdbcClient jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    void insert(Job job) {
        jdbc.sql("""
                INSERT INTO import_jobs (id, business_id, playbook_id, status, source_kind, file_path, source_url)
                VALUES (?, ?, ?, 'queued', ?, ?, ?)""")
                .params(job.id(), job.businessId(), job.playbookId(), job.sourceKind(), job.filePath(), job.sourceUrl())
                .update();
    }

    Optional<Job> find(UUID id) {
        return jdbc.sql("SELECT id, business_id, playbook_id, status, source_kind, file_path, source_url FROM import_jobs WHERE id = ?")
                .param(id)
                .query((rs, i) -> new Job(rs.getObject("id", UUID.class), rs.getObject("business_id", UUID.class),
                        rs.getObject("playbook_id", UUID.class), rs.getString("status"), rs.getString("source_kind"),
                        rs.getString("file_path"), rs.getString("source_url")))
                .optional();
    }

    boolean isActive(UUID playbookId) {
        return jdbc.sql("SELECT count(*) FROM import_jobs WHERE playbook_id = ? AND status IN ('queued', 'running')")
                .param(playbookId)
                .query(Long.class)
                .single() > 0;
    }

    Optional<ImportView> latest(UUID playbookId) {
        return jdbc.sql("""
                SELECT id, status, counts, skipped, notices, error_code FROM import_jobs
                 WHERE playbook_id = ? ORDER BY created_at DESC LIMIT 1""")
                .param(playbookId)
                .query((rs, i) -> new ImportView(rs.getObject("id", UUID.class), rs.getString("status"),
                        rs.getString("counts") == null ? null
                                : json.readValue(rs.getString("counts"), new TypeReference<Map<String, Integer>>() {
                                }),
                        rs.getInt("skipped"),
                        rs.getString("notices") == null ? List.of()
                                : json.readValue(rs.getString("notices"), new TypeReference<List<String>>() {
                                }),
                        rs.getString("error_code")))
                .optional();
    }

    /** Só a execução dona da concessão conclui: com outro token, 0 linhas (AD-038). */
    int finishDone(UUID id, UUID leaseToken, Map<String, Integer> counts, int skipped, List<String> notices) {
        return jdbc.sql("""
                UPDATE import_jobs SET status = 'done', counts = ?::jsonb, skipped = ?, notices = ?::jsonb,
                       finished_at = now(), lease_token = NULL
                 WHERE id = ? AND lease_token = ? AND status = 'running'""")
                .params(json.writeValueAsString(counts), skipped, json.writeValueAsString(notices), id, leaseToken)
                .update();
    }

    int finishFailed(UUID id, UUID leaseToken, String errorCode) {
        return jdbc.sql("""
                UPDATE import_jobs SET status = 'failed', error_code = ?, finished_at = now(), lease_token = NULL
                 WHERE id = ? AND lease_token = ? AND status = 'running'""")
                .params(errorCode, id, leaseToken)
                .update();
    }

    Optional<Claim> claim() {
        return jdbc.sql("SELECT job_id, business_id, lease_token FROM claim_next_import_job(?)")
                .param(LEASE_SECONDS)
                .query((rs, i) -> new Claim(rs.getObject("job_id", UUID.class), rs.getObject("business_id", UUID.class),
                        rs.getObject("lease_token", UUID.class)))
                .optional();
    }

    Map<UUID, String> statuses(List<UUID> ids) {
        Map<UUID, String> result = new HashMap<>();
        jdbc.sql("SELECT job_id, status FROM import_job_statuses(?)")
                .param(ids.toArray(UUID[]::new))
                .query((rs, i) -> result.put(rs.getObject("job_id", UUID.class), rs.getString("status")))
                .list();
        return result;
    }
}
