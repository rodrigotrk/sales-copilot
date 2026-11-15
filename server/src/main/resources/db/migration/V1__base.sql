-- Login e sessão (AD-039, AD-047, AD-048). Sem RLS: só o login usa estas tabelas.
CREATE TABLE users (
    id uuid PRIMARY KEY,
    email text NOT NULL UNIQUE,
    google_subject text UNIQUE,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE allowed_signups (
    email text PRIMARY KEY
);

CREATE TABLE login_link_requests (
    email text PRIMARY KEY,
    requested_at timestamptz NOT NULL
);

-- Formato exigido pelo JdbcOneTimeTokenService do Spring Security.
CREATE TABLE one_time_tokens (
    token_value varchar(36) PRIMARY KEY,
    username varchar(320) NOT NULL,
    expires_at timestamp NOT NULL
);

CREATE TABLE identity_link_challenges (
    id uuid PRIMARY KEY,
    token_hash text NOT NULL UNIQUE,
    email text NOT NULL,
    google_subject text NOT NULL,
    expires_at timestamptz NOT NULL,
    consumed_at timestamptz
);

-- Formato do Spring Session JDBC (schema-postgresql.sql da spring-session-jdbc).
CREATE TABLE spring_session (
    primary_id char(36) NOT NULL,
    session_id char(36) NOT NULL,
    creation_time bigint NOT NULL,
    last_access_time bigint NOT NULL,
    max_inactive_interval int NOT NULL,
    expiry_time bigint NOT NULL,
    principal_name varchar(100),
    CONSTRAINT spring_session_pk PRIMARY KEY (primary_id)
);
CREATE UNIQUE INDEX spring_session_ix1 ON spring_session (session_id);
CREATE INDEX spring_session_ix2 ON spring_session (expiry_time);
CREATE INDEX spring_session_ix3 ON spring_session (principal_name);

CREATE TABLE spring_session_attributes (
    session_primary_id char(36) NOT NULL,
    attribute_name varchar(200) NOT NULL,
    attribute_bytes bytea NOT NULL,
    CONSTRAINT spring_session_attributes_pk PRIMARY KEY (session_primary_id, attribute_name),
    CONSTRAINT spring_session_attributes_fk FOREIGN KEY (session_primary_id) REFERENCES spring_session (primary_id) ON DELETE CASCADE
);

-- Negócios (AD-037, AD-041, AD-045).
CREATE TABLE businesses (
    id uuid PRIMARY KEY,
    name text NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE memberships (
    user_id uuid NOT NULL REFERENCES users (id),
    business_id uuid NOT NULL REFERENCES businesses (id),
    role text NOT NULL CHECK (role IN ('gestor', 'closer')),
    PRIMARY KEY (user_id, business_id)
);

-- Playbook (AD-023, AD-024, AD-036).
CREATE TABLE playbooks (
    id uuid PRIMARY KEY,
    business_id uuid NOT NULL UNIQUE REFERENCES businesses (id),
    name text NOT NULL,
    draft jsonb NOT NULL,
    draft_revision bigint NOT NULL,
    updated_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (id, business_id)
);

CREATE TABLE playbook_versions (
    id uuid PRIMARY KEY,
    playbook_id uuid NOT NULL,
    business_id uuid NOT NULL,
    number int NOT NULL,
    content jsonb NOT NULL,
    published_at timestamptz NOT NULL DEFAULT now(),
    published_by uuid NOT NULL REFERENCES users (id),
    UNIQUE (playbook_id, number),
    FOREIGN KEY (playbook_id, business_id) REFERENCES playbooks (id, business_id)
);

-- Importação (AD-032, AD-038, AD-049).
CREATE TABLE import_jobs (
    id uuid PRIMARY KEY,
    business_id uuid NOT NULL,
    playbook_id uuid NOT NULL,
    status text NOT NULL CHECK (status IN ('queued', 'running', 'done', 'failed')),
    source_kind text NOT NULL CHECK (source_kind IN ('file', 'text', 'google_docs')),
    file_path text,
    source_url text,
    attempts int NOT NULL DEFAULT 0,
    lease_token uuid,
    lease_expires_at timestamptz,
    counts jsonb,
    skipped int NOT NULL DEFAULT 0,
    notices jsonb,
    error_code text,
    created_at timestamptz NOT NULL DEFAULT now(),
    finished_at timestamptz,
    FOREIGN KEY (playbook_id, business_id) REFERENCES playbooks (id, business_id)
);
CREATE UNIQUE INDEX import_jobs_one_active ON import_jobs (playbook_id) WHERE status IN ('queued', 'running');

-- Privilégios do papel app.
GRANT SELECT, INSERT, UPDATE ON users TO app;
GRANT SELECT ON allowed_signups TO app;
GRANT SELECT, INSERT, UPDATE, DELETE ON login_link_requests, one_time_tokens, identity_link_challenges, spring_session, spring_session_attributes TO app;
GRANT SELECT, INSERT ON businesses TO app;
GRANT SELECT, INSERT ON memberships TO app;
GRANT SELECT, INSERT, UPDATE ON playbooks TO app;
GRANT SELECT, INSERT ON playbook_versions TO app;
GRANT SELECT, INSERT, UPDATE ON import_jobs TO app;
GRANT SELECT, UPDATE ON import_jobs TO job_claimer;

-- RLS: sem app.business_id definido, current_setting falha e a consulta não devolve nada.
ALTER TABLE businesses ENABLE ROW LEVEL SECURITY;
ALTER TABLE businesses FORCE ROW LEVEL SECURITY;
CREATE POLICY businesses_of_member ON businesses FOR SELECT TO app
    USING (id IN (SELECT business_id FROM memberships WHERE user_id = current_setting('app.user_id')::uuid));
CREATE POLICY businesses_create ON businesses FOR INSERT TO app
    WITH CHECK (id = current_setting('app.business_id')::uuid);

ALTER TABLE memberships ENABLE ROW LEVEL SECURITY;
ALTER TABLE memberships FORCE ROW LEVEL SECURITY;
CREATE POLICY memberships_of_user ON memberships FOR SELECT TO app
    USING (user_id = current_setting('app.user_id')::uuid);
CREATE POLICY memberships_create ON memberships FOR INSERT TO app
    WITH CHECK (user_id = current_setting('app.user_id')::uuid
        AND business_id = current_setting('app.business_id')::uuid);

ALTER TABLE playbooks ENABLE ROW LEVEL SECURITY;
ALTER TABLE playbooks FORCE ROW LEVEL SECURITY;
CREATE POLICY playbooks_of_business ON playbooks TO app
    USING (business_id = current_setting('app.business_id')::uuid)
    WITH CHECK (business_id = current_setting('app.business_id')::uuid);

ALTER TABLE playbook_versions ENABLE ROW LEVEL SECURITY;
ALTER TABLE playbook_versions FORCE ROW LEVEL SECURITY;
CREATE POLICY playbook_versions_of_business ON playbook_versions TO app
    USING (business_id = current_setting('app.business_id')::uuid)
    WITH CHECK (business_id = current_setting('app.business_id')::uuid);

ALTER TABLE import_jobs ENABLE ROW LEVEL SECURITY;
ALTER TABLE import_jobs FORCE ROW LEVEL SECURITY;
CREATE POLICY import_jobs_of_business ON import_jobs TO app
    USING (business_id = current_setting('app.business_id')::uuid)
    WITH CHECK (business_id = current_setting('app.business_id')::uuid);
CREATE POLICY import_jobs_claimer ON import_jobs TO job_claimer
    USING (true) WITH CHECK (true);

-- Pega a próxima tarefa de qualquer negócio (AD-049): devolve só o necessário para o worker
-- seguir sob RLS com o negócio da tarefa.
CREATE FUNCTION claim_next_import_job(lease_seconds int)
    RETURNS TABLE (job_id uuid, business_id uuid, lease_token uuid)
    LANGUAGE sql SECURITY DEFINER
    SET search_path = pg_catalog, sales_copilot
AS $$
    UPDATE import_jobs j
       SET status = 'running',
           lease_token = gen_random_uuid(),
           lease_expires_at = now() + make_interval(secs => lease_seconds),
           attempts = j.attempts + 1
     WHERE j.id = (SELECT q.id FROM import_jobs q
                    WHERE q.status = 'queued'
                       OR (q.status = 'running' AND q.lease_expires_at < now())
                    ORDER BY q.created_at
                    FOR UPDATE SKIP LOCKED
                    LIMIT 1)
    RETURNING j.id, j.business_id, j.lease_token;
$$;

-- A varredura de arquivos precisa saber o estado de tarefas de qualquer negócio.
CREATE FUNCTION import_job_statuses(job_ids uuid[])
    RETURNS TABLE (job_id uuid, status text)
    LANGUAGE sql SECURITY DEFINER
    SET search_path = pg_catalog, sales_copilot
AS $$
    SELECT id, status FROM import_jobs WHERE id = ANY (job_ids);
$$;

ALTER FUNCTION claim_next_import_job(int) OWNER TO job_claimer;
ALTER FUNCTION import_job_statuses(uuid[]) OWNER TO job_claimer;
REVOKE EXECUTE ON FUNCTION claim_next_import_job(int) FROM PUBLIC;
REVOKE EXECUTE ON FUNCTION import_job_statuses(uuid[]) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION claim_next_import_job(int) TO app;
GRANT EXECUTE ON FUNCTION import_job_statuses(uuid[]) TO app;
