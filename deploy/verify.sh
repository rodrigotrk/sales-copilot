#!/bin/sh
# Prova do C99: o Compose tem Postgres, servidor e Caddy, e o Caddy, rodando com o Caddyfile real,
# entrega ao servidor todas as rotas da API, do login e do logout, e entrega o app nas demais.
set -eu
cd "$(dirname "$0")"
services=$(docker compose -f docker-compose.yml --env-file .env.example config --services | sort | tr '\n' ' ')
[ "$services" = "caddy postgres server " ] || { echo "serviços inesperados: $services"; exit 1; }

run=verify-deploy-$$
cleanup() {
	docker rm -f "$run-server" "$run-caddy" >/dev/null 2>&1 || true
	docker network rm "$run" >/dev/null 2>&1 || true
}
trap cleanup EXIT
docker network create "$run" >/dev/null
# No lugar do Spring, um servidor que responde "servidor" em qualquer caminho.
docker run -d --name "$run-server" --network "$run" --network-alias server caddy:2 \
	caddy respond --listen :8080 --body servidor >/dev/null
docker run -d --name "$run-caddy" --network "$run" -e SITE_ADDRESS=:80 \
	-v "$PWD/Caddyfile:/etc/caddy/Caddyfile:ro" caddy:2 >/dev/null
docker exec "$run-caddy" sh -c 'mkdir -p /srv && echo app > /srv/index.html'

body() {
	for _ in 1 2 3 4 5 6 7 8 9 10; do
		if out=$(docker exec "$run-caddy" wget -qO- "http://localhost$1" 2>/dev/null); then
			echo "$out"
			return
		fi
		sleep 1
	done
	echo "sem resposta"
}

# Todas as rotas do Surface do plano, e um caminho qualquer sob cada prefixo, para um matcher
# estreito demais (só /api/me, por exemplo) não passar.
B=/api/businesses/b1/playbook
for path in /api/me /api/businesses $B $B/draft $B/versions $B/imports /api/login-links/t1 /api/identity-links/t1 \
	/ott/generate /login/ott /login/identity-link /logout /oauth2/authorization/google /login/oauth2/code/google \
	/api/qualquer/caminho /ott/qualquer /oauth2/qualquer/caminho /login/oauth2/qualquer/caminho; do
	got=$(body "$path")
	[ "$got" = servidor ] || { echo "rota não encaminhada ao servidor: $path ($got)"; exit 1; }
done
for path in / /negocios /n/b1/playbook /entrar; do
	got=$(body "$path")
	[ "$got" = app ] || { echo "rota do app não serve o index.html: $path ($got)"; exit 1; }
done
echo "deploy ok"
