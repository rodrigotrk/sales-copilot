#!/bin/sh
# Prova do C99: o Compose tem Postgres, servidor e Caddy, e o Caddy, na configuração que ele mesmo
# gera a partir do Caddyfile, encaminha ao Spring as rotas da API, do login e do logout.
set -eu
cd "$(dirname "$0")"
services=$(docker compose -f docker-compose.yml --env-file .env.example config --services | sort | tr '\n' ' ')
[ "$services" = "caddy postgres server " ] || { echo "serviços inesperados: $services"; exit 1; }
docker run --rm -v "$PWD/Caddyfile:/etc/caddy/Caddyfile:ro" caddy:2 \
	caddy adapt --config /etc/caddy/Caddyfile --adapter caddyfile --validate 2>/dev/null |
	python3 -c '
import json, sys

required = ["/api/*", "/ott/*", "/login/ott", "/login/identity-link", "/logout", "/oauth2/*", "/login/oauth2/*"]

def proxies_to_server(handlers):
    for handler in handlers:
        if handler.get("handler") == "reverse_proxy" and any(u.get("dial") == "server:8080" for u in handler.get("upstreams", [])):
            return True
        if any(proxies_to_server(route.get("handle", [])) for route in handler.get("routes", [])):
            return True
    return False

def proxied_paths(routes):
    paths = set()
    for route in routes:
        if proxies_to_server(route.get("handle", [])):
            paths.update(p for m in route.get("match", []) for p in m.get("path", []))
        for handler in route.get("handle", []):
            paths |= proxied_paths(handler.get("routes", []))
    return paths

config = json.load(sys.stdin)
routes = [r for server in config["apps"]["http"]["servers"].values() for r in server["routes"]]
missing = [p for p in required if p not in proxied_paths(routes)]
if missing:
    sys.exit("rotas sem encaminhamento ao servidor: " + " ".join(missing))
'
echo "deploy ok"
