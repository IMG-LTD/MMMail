# MMMail Community Edition Install Quickstart

**Version**: `v2.0.3`
**Date**: `2026-04-23`

## Current baseline
- Public baseline: `main` / `v2.0.3`
- Repository mainline: v2-only code and runtime paths
- Default self-hosted runtime: `frontend-v2 Web + single Spring Boot backend + MySQL / Redis`
- Standard mode may include `Nacos`, but that is not a promise of a production microservice mesh
- Support boundaries: `../release/v2-support-boundaries.md`
- Module maturity: `../open-source/module-maturity-matrix.md`

## Recommended first path: minimal self-hosted mode
1. Copy `.env.example` to `.env`
2. Keep `MMMAIL_NACOS_ENABLED=false`
3. Keep or set `VITE_API_BASE_URL=http://localhost:8080`
4. Replace at least:
   - `MMMAIL_JWT_SECRET`
   - `SPRING_DATASOURCE_PASSWORD`
   - `SPRING_REDIS_PASSWORD`
   - `MYSQL_ROOT_PASSWORD`
5. Validate the environment:
   - `./scripts/validate-runtime-env.sh .env`
6. Start the stack:
   - `docker compose --env-file .env -f docker-compose.minimal.yml up -d --build`

## Need standard mode later?
- Set `MMMAIL_NACOS_ENABLED=true`
- Replace `NACOS_USERNAME` and `NACOS_PASSWORD`
- Start with `docker compose --env-file .env up -d --build`

## Validate
- Frontend: `http://127.0.0.1:3001`
- Backend health: `http://127.0.0.1:8080/actuator/health`
- Swagger UI: `http://127.0.0.1:8080/swagger-ui.html`
- Product boundary: `http://127.0.0.1:3001/boundary`
- System health: `http://127.0.0.1:3001/settings/system-health`
- Migration status:
  - `./scripts/db-upgrade.sh .env info`

## Local development
- Install frontend deps:
  - `pnpm --dir frontend-v2 install`
- Start the frontend dev server:
  - `pnpm --dir frontend-v2 dev`
- Default local URL:
  - `http://127.0.0.1:5174`
- Default validation gate:
  - `bash scripts/validate-local.sh`
