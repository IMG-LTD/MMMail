# MMMail v2 Mainline First-time Install Guide

**Version**: `v2.0.3`
**Date**: `2026-04-24`

## Current boundaries and prerequisites
- Public baseline: `main` / `v2.0.3`; the repository mainline contains v2-only code and runtime paths.
- Default self-hosted runtime: `frontend-v2 Web + single Spring Boot backend + MySQL / Redis`.
- Standard mode may additionally enable `Nacos`, but this is not a promise of a production microservice mesh.
- Docker installs require `Docker` and `Docker Compose v2`; at least `4 CPU / 8 GB RAM` is recommended.
- Minimal mode ports: `3001`, `8080`, `3306`, `6379`; standard mode additionally uses `8848`.
- Support boundaries: `../release/v2-support-boundaries.md`; module maturity: `../open-source/module-maturity-matrix.md`.

## 1. Choose an install path
Choose one path before the first deployment:

| Path | When to use it | Runtime shape |
| --- | --- | --- |
| One-click install | You want scripts to validate the environment and start Compose | Minimal or standard mode |
| Docker manual install | You want to run Compose commands and inspect status/logs yourself | Minimal or standard mode |
| Bare-metal manual install | You do not use Docker, or you need existing hosts, databases, or frontend hosting | MySQL 8.4 + Redis 7.4 + Java backend + built frontend assets |
| Local experience / development | You contribute code, quickly try the frontend, or debug a local API | Vite dev server + local backend/dependencies |

Mode boundaries:
- Minimal mode: `MySQL + Redis + backend + frontend`, no Nacos; `.env` must keep `MMMAIL_NACOS_ENABLED=false`, and the Compose file is `docker-compose.minimal.yml`.
- Standard mode: includes `Nacos` in addition to the minimal stack; `.env` must set `MMMAIL_NACOS_ENABLED=true`, and the Compose file is the default `docker-compose.yml`.

All self-hosted paths should start from the environment template:
1. Copy the template: `cp .env.example .env`
2. Edit `.env` and replace at least these placeholders:
   - `MMMAIL_JWT_SECRET`
   - `SPRING_DATASOURCE_PASSWORD`
   - `SPRING_REDIS_PASSWORD`
   - `MYSQL_ROOT_PASSWORD`
3. Standard mode also requires replacing:
   - `NACOS_USERNAME`
   - `NACOS_PASSWORD`
4. Keep `SPRING_SQL_INIT_MODE=never`; Flyway owns schema migrations.

## 2. One-click install
The one-click scripts check the environment file, validate required variables, and call the matching Docker Compose file.

Linux / macOS:
- Minimal mode: `bash scripts/install.sh minimal`
- Standard mode: `bash scripts/install.sh standard`

Windows PowerShell:
- Minimal mode: `./scripts/install.ps1 minimal`
- Standard mode: `./scripts/install.ps1 standard`

Notes:
- If no mode is passed, the script tries to ask interactively; non-interactive shells default to minimal mode.
- The default env file is `.env` in the repository root. To use another file, set `MMMAIL_ENV_FILE=/path/to/.env`; PowerShell can also use `-EnvFile path`.
- The scripts do not create production-grade secrets for you. If `.env` does not exist, they create it from `.env.example` and exit; replace placeholders before running again.
- Minimal mode requires `MMMAIL_NACOS_ENABLED=false`; standard mode requires `MMMAIL_NACOS_ENABLED=true`.

## 3. Docker manual install
### Minimal mode
Minimal mode is recommended for the first self-hosted validation. It starts only MySQL, Redis, the backend, and the frontend, without Nacos.

1. Check `.env`:
   - `MMMAIL_NACOS_ENABLED=false`
   - `VITE_API_BASE_URL=http://localhost:8080`
   - Required secrets and passwords have replaced placeholder values
2. Start:
   - `docker compose --env-file .env -f docker-compose.minimal.yml up -d --build`
3. Check status:
   - `docker compose -f docker-compose.minimal.yml ps`
4. Inspect logs:
   - `docker compose -f docker-compose.minimal.yml logs -f backend`
   - `docker compose -f docker-compose.minimal.yml logs -f frontend`
5. Stop:
   - `docker compose -f docker-compose.minimal.yml down`

### Standard mode
Standard mode additionally includes Nacos in Compose. Use it when you need to validate the Nacos integration boundary.

1. Check `.env`:
   - `MMMAIL_NACOS_ENABLED=true`
   - `NACOS_USERNAME` and `NACOS_PASSWORD` have replaced placeholder values
   - Other required secrets and passwords have replaced placeholder values
2. Start:
   - `docker compose --env-file .env up -d --build`
3. Check status:
   - `docker compose ps`
4. Inspect logs:
   - `docker compose logs -f backend`
   - `docker compose logs -f frontend`
5. Stop:
   - `docker compose down`

You can run the runtime validator before starting:
- `./scripts/validate-runtime-env.sh .env`

If you need to remove volumes too, first confirm the data can be discarded:
- Minimal mode: `docker compose -f docker-compose.minimal.yml down -v`
- Standard mode: `docker compose down -v`

## 4. Bare-metal manual install
Use the bare-metal path when installing without Docker. These are high-level steps; your operations environment must add hardening, process supervision, reverse proxying, TLS, and backups.

1. Prepare dependencies:
   - MySQL `8.4`, with an `mmmail` database and application user.
   - Redis `7.4`, with a password and persistence enabled.
   - Java `21` and Maven.
   - Node.js / pnpm for building `frontend-v2`.
2. Prepare environment variables:
   - `cp .env.example .env`
   - Replace every `replace-with-*` placeholder.
   - Set `SPRING_DATASOURCE_URL` for the bare-metal MySQL instance.
   - Set `SPRING_REDIS_HOST`, `SPRING_REDIS_PORT`, and `SPRING_REDIS_PASSWORD` for the bare-metal Redis instance.
   - Without Nacos, set `MMMAIL_NACOS_ENABLED=false`; with Nacos, prepare Nacos and set `MMMAIL_NACOS_ENABLED=true`, `NACOS_SERVER_ADDR`, `NACOS_USERNAME`, and `NACOS_PASSWORD`.
   - Keep `SPRING_SQL_INIT_MODE=never`; Flyway owns schema migrations.
3. Start the backend:
   - Use the local helper: `bash scripts/start-backend-local.sh`
   - Or run Maven Spring Boot from the `backend` directory: `mvn -pl mmmail-server -am -DskipTests spring-boot:run`
   - For production-like hosting, manage the Java process with systemd, supervisor, or equivalent platform tooling.
4. Build and serve the frontend:
   - `pnpm --dir frontend-v2 install`
   - `pnpm --dir frontend-v2 build`
   - Serve `frontend-v2/dist` through a static file server or reverse proxy.
   - Ensure `VITE_API_BASE_URL` points to the backend address at build time.
5. Database migrations:
   - Check migration status: `./scripts/db-upgrade.sh .env info`
   - Run upgrades when needed: `./scripts/db-upgrade.sh .env upgrade`

## 5. Local experience / development
Local experience is for development, debugging, or quickly trying the frontend. The default frontend dev port is `5174`; the Compose-hosted frontend port is `3001`.

Frontend dev server:
- `pnpm --dir frontend-v2 install`
- `pnpm --dir frontend-v2 dev`
- Default URL: `http://127.0.0.1:5174`

Local backend:
- Prepare MySQL, Redis, and `.env` first.
- Confirm `VITE_API_BASE_URL=http://localhost:8080`.
- Start the backend: `bash scripts/start-backend-local.sh`
- Default backend health URL: `http://127.0.0.1:8080/actuator/health`

Local validation:
- `bash scripts/validate-local.sh`
- Frontend tests: `pnpm --dir frontend-v2 test`

## 6. Verification
After installing or starting, verify at least:
- Frontend: `http://127.0.0.1:3001`
- Backend health: `http://127.0.0.1:8080/actuator/health`
- Boundary page: `http://127.0.0.1:3001/boundary`
- Migration info: `./scripts/db-upgrade.sh .env info`

Optional checks:
- Swagger UI: `http://127.0.0.1:8080/swagger-ui.html`
- OpenAPI: `http://127.0.0.1:8080/v3/api-docs`
- System health: `http://127.0.0.1:3001/settings/system-health`

## 7. FAQ
- `validate-runtime-env.sh` fails:
  - Check whether `.env` still contains `replace-with-*` placeholders or is missing required variables.
- Minimal mode still asks for Nacos credentials:
  - Confirm `.env` has `MMMAIL_NACOS_ENABLED=false`, and use `docker-compose.minimal.yml` or `scripts/install.sh minimal` / `scripts/install.ps1 minimal`.
- Standard mode starts but the backend cannot connect to Nacos:
  - Confirm you are using the default `docker-compose.yml`, and set `MMMAIL_NACOS_ENABLED=true`, `NACOS_USERNAME`, and `NACOS_PASSWORD`.
- The frontend opens but calls the wrong API:
  - Check `VITE_API_BASE_URL`, then rebuild the frontend image or restart the Vite dev server.
- The backend cannot connect to MySQL:
  - Check `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, and the MySQL listen address.
- Database schema is missing or inconsistent:
  - Do not enable Spring SQL initialization; keep `SPRING_SQL_INIT_MODE=never`, and use `./scripts/db-upgrade.sh .env info` to inspect Flyway status.
