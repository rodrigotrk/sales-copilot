#!/bin/sh
# Prova do C104: o script de papéis cria migrator, app e job_claimer com senhas que têm apóstrofo,
# aspas e ponto e vírgula, e cada papel com login entra pela rede com a própria senha e é recusado com
# uma senha errada.
set -eu
cd "$(dirname "$0")"
name=verify-roles-$$
volumes_before=$(docker volume ls -q | sort)
trap 'docker rm -f -v "$name" >/dev/null 2>&1 || true; docker network rm "$name" >/dev/null 2>&1 || true' EXIT
docker network create "$name" >/dev/null
migrator_password="mig'ra\"tor; DROP ROLE x; --"
app_password="a'p\"p;'"
docker run -d --name "$name" --network "$name" --network-alias db -e POSTGRES_PASSWORD=admin -e POSTGRES_DB=salescopilot \
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
# job_claimer só é dono das funções privilegiadas: nenhum login com ele (AD-049).
claimer_login=$(docker exec "$name" psql -U postgres -d salescopilot -Atc "SELECT rolcanlogin FROM pg_roles WHERE rolname = 'job_claimer'")
[ "$claimer_login" = f ] || { echo "job_claimer aceita login"; exit 1; }
# As senhas são conferidas de outro contêiner: dentro do próprio contêiner o Postgres aceita o loopback
# sem senha (trust), e só a rede exige scram-sha-256, como a conexão do servidor em produção.
login() {
	docker run --rm --network "$name" -e PGPASSWORD="$2" postgres:17 \
		psql -h db -U "$1" -d salescopilot -Atc 'SHOW search_path' 2>/dev/null
}
for pair in "migrator:$migrator_password" "app:$app_password"; do
	role=${pair%%:*}
	password=${pair#*:}
	got=$(login "$role" "$password") || got="recusado"
	[ "$got" = sales_copilot ] || { echo "$role não entrou com a própria senha ($got)"; exit 1; }
	if login "$role" "senha-errada" >/dev/null; then
		echo "$role entrou com uma senha errada"
		exit 1
	fi
done
# A prova não deixa volume do Postgres para trás.
docker rm -f -v "$name" >/dev/null
[ "$(docker volume ls -q | sort)" = "$volumes_before" ] || { echo "volume do Postgres ficou para trás"; exit 1; }
echo "roles ok"
