#!/bin/sh
# Prova do C99: o Compose tem Postgres, servidor e Caddy, e o Caddy, rodando com o Caddyfile real,
# entrega ao servidor todas as rotas da API, do login e do logout, e entrega o app nas demais.
set -eu
cd "$(dirname "$0")"
services=$(docker compose -f docker-compose.yml --env-file .env.example config --services | sort | tr '\n' ' ')
[ "$services" = "caddy postgres server " ] || { echo "serviços inesperados: $services"; exit 1; }

run=verify-deploy-$$
cleanup() {
	docker rm -f "$run-server" "$run-caddy" "$run-client" >/dev/null 2>&1 || true
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
# Cliente com curl na mesma rede, para mandar cada rota com o método do Surface.
docker run -d --name "$run-client" --network "$run" --entrypoint sleep curlimages/curl:8.11.1 600 >/dev/null

body() {
	for _ in 1 2 3 4 5 6 7 8 9 10; do
		if out=$(docker exec "$run-client" curl -fsS -X "$1" "http://$run-caddy$2" 2>/dev/null); then
			echo "$out"
			return
		fi
		sleep 1
	done
	echo "sem resposta"
}

# Todas as rotas do Surface do plano com o método de cada uma, e cinco métodos sob cada prefixo,
# para um matcher estreito demais (só /api/me, só GET) não passar.
B=/api/businesses/b1/playbook
for route in "GET /api/me" "POST /api/businesses" "GET $B" "PUT $B/draft" "POST $B/versions" "POST $B/imports" \
	"GET /api/login-links/t1" "GET /api/identity-links/t1" "POST /ott/generate" "POST /login/ott" \
	"POST /login/identity-link" "POST /logout" "GET /oauth2/authorization/google" "GET /login/oauth2/code/google"; do
	got=$(body $route)
	[ "$got" = servidor ] || { echo "rota não encaminhada ao servidor: $route ($got)"; exit 1; }
done
for method in GET POST PUT PATCH DELETE; do
	for path in /api/qualquer/caminho /ott/qualquer /oauth2/qualquer/caminho /login/oauth2/qualquer/caminho; do
		got=$(body "$method" "$path")
		[ "$got" = servidor ] || { echo "rota não encaminhada ao servidor: $method $path ($got)"; exit 1; }
	done
done
for path in / /negocios /n/b1/playbook /entrar; do
	got=$(body GET "$path")
	[ "$got" = app ] || { echo "rota do app não serve o index.html: $path ($got)"; exit 1; }
done
echo "deploy ok"
