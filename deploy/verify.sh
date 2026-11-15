#!/bin/sh
# Prova do C99: o Compose tem Postgres, servidor e Caddy, e o Caddyfile é válido e encaminha ao
# Spring as rotas da API, do login e do logout.
set -eu
cd "$(dirname "$0")"
services=$(docker compose -f docker-compose.yml --env-file .env.example config --services | sort | tr '\n' ' ')
[ "$services" = "caddy postgres server " ] || { echo "serviços inesperados: $services"; exit 1; }
docker run --rm -v "$PWD/Caddyfile:/etc/caddy/Caddyfile:ro" caddy:2 \
	caddy validate --config /etc/caddy/Caddyfile --adapter caddyfile
for route in /api/* /ott/* /login/ott /login/identity-link /logout /oauth2/* /login/oauth2/*; do
	grep -qF "$route" Caddyfile || { echo "rota sem encaminhamento: $route"; exit 1; }
done
grep -qF "reverse_proxy server:8080" Caddyfile || { echo "Caddyfile sem reverse_proxy para o servidor"; exit 1; }
echo "deploy ok"
