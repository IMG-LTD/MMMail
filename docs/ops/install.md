# MMMail v2 Mainline 安装说明

**版本**: `v2.0.3`
**日期**: `2026-04-23`

## 当前边界
- 当前公开基线为 `main` / `v2.0.3`。
- 当前仓库主线只保留 v2 代码与运行路径。
- 默认自托管运行模型为 `frontend-v2 Web + 单个 Spring Boot 后端进程 + MySQL / Redis`。
- 标准模式可额外启用 `Nacos`，但这不代表仓库已经交付真实微服务网格。
- 权威范围见 `docs/release/v2-support-boundaries.md` 与 `docs/open-source/module-maturity-matrix.md`。

## 前置条件
- `Docker` + `Docker Compose v2`
- 至少 `4 CPU / 8 GB RAM`
- 最小模式端口：`3001`、`8080`、`3306`、`6379`
- 标准模式额外端口：`8848`

## 1. 准备运行时环境
1. 复制模板：
   - `cp .env.example .env`
2. 编辑 `.env`，至少替换以下占位值：
   - `MMMAIL_JWT_SECRET`
   - `SPRING_DATASOURCE_PASSWORD`
   - `SPRING_REDIS_PASSWORD`
   - `MYSQL_ROOT_PASSWORD`
3. 首次采用建议保持：
   - `MMMAIL_NACOS_ENABLED=false`
   - `VITE_API_BASE_URL=http://localhost:8080`

## 2. 启动前校验
- 执行：
  - `./scripts/validate-runtime-env.sh .env`

校验通过后再继续启动；若脚本提示 `placeholder` 或 `missing`，先修正 `.env`。

## 3. 推荐首次采用：最小模式
- 启动：
  - `docker compose --env-file .env -f docker-compose.minimal.yml up -d --build`
- 查看状态：
  - `docker compose -f docker-compose.minimal.yml ps`
- 查看日志：
  - `docker compose -f docker-compose.minimal.yml logs -f backend`
  - `docker compose -f docker-compose.minimal.yml logs -f frontend`

## 4. 如需标准模式再启用 Nacos
- 设置 `.env` 中 `MMMAIL_NACOS_ENABLED=true`
- 额外替换：
  - `NACOS_USERNAME`
  - `NACOS_PASSWORD`
- 启动：
  - `docker compose --env-file .env up -d --build`
- 查看状态：
  - `docker compose ps`
- 查看日志：
  - `docker compose logs -f backend`
  - `docker compose logs -f frontend`

## 5. 验证服务
- Frontend：
  - `http://127.0.0.1:3001`
- Backend health：
  - `curl -sf http://127.0.0.1:8080/actuator/health`
- Swagger UI：
  - `http://127.0.0.1:8080/swagger-ui.html`
- OpenAPI：
  - `curl -sf http://127.0.0.1:8080/v3/api-docs`
- Boundary：
  - `http://127.0.0.1:3001/boundary`
- System health：
  - `http://127.0.0.1:3001/settings/system-health`
- 数据迁移状态：
  - `./scripts/db-upgrade.sh .env info`

## 6. 本地开发入口
- Frontend v2：
  - `pnpm --dir frontend-v2 install`
  - `pnpm --dir frontend-v2 dev`
  - 默认地址：`http://127.0.0.1:5174`
- 默认前端 API 目标：
  - `VITE_API_BASE_URL=http://localhost:8080`
- 本地默认门禁：
  - `bash scripts/validate-local.sh`

## 7. 停止与清理
- 停止：
  - 标准模式：`docker compose down`
  - 最小模式：`docker compose -f docker-compose.minimal.yml down`
- 连同卷一起清理：
  - 标准模式：`docker compose down -v`
  - 最小模式：`docker compose -f docker-compose.minimal.yml down -v`

## 8. 常见问题
- `validate-runtime-env.sh` 失败：
  - `.env` 仍保留 `replace-with-*` 占位值。
- 最小模式仍要求 Nacos 凭据：
  - 确认 `.env` 中 `MMMAIL_NACOS_ENABLED=false`，然后重新执行 `./scripts/validate-runtime-env.sh .env`。
- Frontend 页面可打开但 API 指向错误：
  - 检查 `VITE_API_BASE_URL` 是否仍指向旧环境。
- Backend 无法连接 MySQL：
  - 检查 `SPRING_DATASOURCE_PASSWORD` 与 `MYSQL_ROOT_PASSWORD` 是否已正确设置。
