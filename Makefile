PNPM := COREPACK_ENABLE_DOWNLOAD_PROMPT=0 corepack pnpm
MVNW := cd server && ./mvnw -B

.PHONY: doctor build lint test-server test-web check e2e-server

doctor:
	@java -version 2>&1 | grep -q 'version "25' && echo "ok   Java 25" || echo "falta Java 25"
	@docker info >/dev/null 2>&1 && echo "ok   Docker em execução" || echo "falta Docker em execução"
	@node -v 2>/dev/null | grep -q '^v24' && echo "ok   Node 24" || echo "falta Node 24"
	@cd web && $(PNPM) --version >/dev/null 2>&1 && echo "ok   pnpm via corepack" || echo "falta pnpm via corepack"

build:
	$(MVNW) -q -DskipTests package
	cd web && $(PNPM) build

lint:
	cd web && $(PNPM) exec oxlint . && $(PNPM) exec eslint .

test-server:
	$(MVNW) test

test-web:
	cd web && $(PNPM) exec vitest run --reporter=verbose

check: build lint test-server test-web

e2e-server:
	$(MVNW) spring-boot:test-run -Dspring-boot.run.profiles=e2e
