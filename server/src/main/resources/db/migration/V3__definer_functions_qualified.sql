-- As funções SECURITY DEFINER rodam como job_claimer. Sem pg_temp explícito no search_path, o
-- PostgreSQL procura relações em pg_temp primeiro, e uma tabela temporária "import_jobs" criada pelo
-- papel app tomaria o lugar da real. As tabelas vão qualificadas e pg_temp fica por último.
CREATE OR REPLACE FUNCTION claim_next_import_job(lease_seconds int)
    RETURNS TABLE (job_id uuid, business_id uuid, lease_token uuid)
    LANGUAGE sql SECURITY DEFINER
    SET search_path = pg_catalog, sales_copilot, pg_temp
AS $$
    UPDATE sales_copilot.import_jobs j
       SET status = 'running',
           lease_token = gen_random_uuid(),
           lease_expires_at = now() + make_interval(secs => lease_seconds),
           attempts = j.attempts + 1
     WHERE j.id = (SELECT q.id FROM sales_copilot.import_jobs q
                    WHERE q.status = 'queued'
                       OR (q.status = 'running' AND q.lease_expires_at < now())
                    ORDER BY q.created_at
                    FOR UPDATE SKIP LOCKED
                    LIMIT 1)
    RETURNING j.id, j.business_id, j.lease_token;
$$;

CREATE OR REPLACE FUNCTION import_job_statuses(job_ids uuid[])
    RETURNS TABLE (job_id uuid, status text)
    LANGUAGE sql SECURITY DEFINER
    SET search_path = pg_catalog, sales_copilot, pg_temp
AS $$
    SELECT id, status FROM sales_copilot.import_jobs WHERE id = ANY (job_ids);
$$;
