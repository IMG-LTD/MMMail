# MMMail Community Edition Install Quickstart

## Recommended first path: minimal self-hosted mode
1. Copy `.env.example` to `.env`
2. Keep `MMMAIL_NACOS_ENABLED=false`
3. Replace at least:
   - `MMMAIL_JWT_SECRET`
   - `SPRING_DATASOURCE_PASSWORD`
   - `SPRING_REDIS_PASSWORD`
   - `MYSQL_ROOT_PASSWORD`
4. Validate the environment:
   - `./scripts/validate-runtime-env.sh .env`
5. Start the stack:
   - `docker compose --env-file .env -f docker-compose.minimal.yml up -d --build`

## Need standard mode later?
- Set `MMMAIL_NACOS_ENABLED=true`
- Replace `NACOS_USERNAME` and `NACOS_PASSWORD`
- Start with `docker compose --env-file .env up -d --build`

## Validate
- Frontend: `http://127.0.0.1:3001`
- Backend: `http://127.0.0.1:8080/actuator/health`
- Product boundary: `http://127.0.0.1:3001/suite?section=boundary`
