#!/bin/sh
# Papéis da AD-037 e AD-049: migrator é dono das tabelas e roda o Flyway; app é o papel do servidor,
# sem posse nem BYPASSRLS; job_claimer só é dono da função que pega tarefas de qualquer negócio.
set -eu
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<SQL
CREATE ROLE migrator LOGIN PASSWORD '${MIGRATOR_PASSWORD}';
CREATE ROLE app LOGIN PASSWORD '${APP_PASSWORD}';
CREATE ROLE job_claimer NOLOGIN;
GRANT job_claimer TO migrator;
CREATE SCHEMA sales_copilot AUTHORIZATION migrator;
GRANT USAGE ON SCHEMA sales_copilot TO app, job_claimer;
GRANT CREATE ON SCHEMA sales_copilot TO job_claimer;
ALTER ROLE migrator SET search_path TO sales_copilot;
ALTER ROLE app SET search_path TO sales_copilot;
SQL
