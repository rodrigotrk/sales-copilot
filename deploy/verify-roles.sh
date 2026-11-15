#!/bin/sh
# Prova do C104: o script de papéis cria migrator, app e job_claimer com senhas que têm apóstrofo,
# aspas e ponto e vírgula, e cada papel com login entra com a própria senha.
set -eu
cd "$(dirname "$0")"
name=verify-roles-$$
volumes_before=$(docker volume ls -q | sort)
trap 'docker rm -f -v "$name" >/dev/null 2>&1 || true' EXIT
migrator_password="mig'ra\"tor; DROP ROLE x; --"
app_password="a'p\"p;'"
docker run -d --name "$name" -e POSTGRES_PASSWORD=admin -e POSTGRES_DB=salescopilot \
	-e MIGRATOR_PASSWORD="$migrator_password" -e APP_PASSWORD="$app_password" \
	-v "$PWD/postgres/initdb:/docker-entrypoint-initdb.d:ro" postgres:17 >/dev/null

# O servidor só escuta em TCP depois que a inicialização termina.
ready=no
for _ in $(seq 1 60); do
	if docker exec "$name" pg_isready -h 127.0.0.1 -U postgres >/dev/null 2>&1; then ready=yes; break; fi
	sleep 1
done
[ "$ready" = yes ] || { echo "postgres não ficou pronto"; docker logs "$name" 2>&1 | tail -5; exit 1; }

roles=$(docker exec "$name" psql -U postgres -d salescopilot -Atc \
	"SELECT string_agg(rolname, ' ' ORDER BY rolname) FROM pg_roles WHERE rolname IN ('app', 'job_claimer', 'migrator')")
[ "$roles" = "app job_claimer migrator" ] || { echo "papéis criados: $roles"; exit 1; }
for pair in "migrator:$migrator_password" "app:$app_password"; do
	role=${pair%%:*}
	password=${pair#*:}
	got=$(docker exec -e PGPASSWORD="$password" "$name" psql -h 127.0.0.1 -U "$role" -d salescopilot -Atc 'SHOW search_path')
	[ "$got" = sales_copilot ] || { echo "$role entrou com search_path $got"; exit 1; }
done
# A prova não deixa volume do Postgres para trás.
docker rm -f -v "$name" >/dev/null
[ "$(docker volume ls -q | sort)" = "$volumes_before" ] || { echo "volume do Postgres ficou para trás"; exit 1; }
echo "roles ok"
