# MMMail v2 Mainline 运维 Runbook

**版本**: `v2.0.4`
**日期**: `2026-04-23`

## 1. 入口清单
- 健康检查：`GET /actuator/health`
- Prometheus 导出：`GET /actuator/prometheus`
- 管理页：`/settings/system-health`
- API 文档：
  - `GET /v3/api-docs`
  - `/swagger-ui.html`
- 本地门禁：`bash scripts/validate-local.sh`
- CI 门禁：`.github/workflows/ci.yml`

## 2. 核心检查
### 服务存活
- Backend：
  - `curl -sf http://127.0.0.1:8080/actuator/health`
- Frontend：
  - 访问 `http://127.0.0.1:3001`
- 系统健康页：
  - 使用管理员账号登录后访问 `/settings/system-health`
- Boundary：
  - 打开 `/boundary`
  - 确认 `GA / Beta / Preview` 说明与 `docs/release/v2-support-boundaries.md` 一致
- Module maturity：
  - 对照 `docs/open-source/module-maturity-matrix.md`

### 当前主线运行面
- `Mail / Calendar / Drive / Business / Organizations / Security / Settings` 为正式主线检查面
- `Pass / Docs / Sheets` 为默认可见但按 `Beta` 口径维护
- `Collaboration / Command Center / Notifications / Labs` 为 `Preview`

### 本地门禁环境
- `bash scripts/validate-local.sh` 会显式执行：
  - `frontend-v2` tests
  - `frontend-v2` contract regression
  - `frontend-v2` typecheck
  - `frontend-v2` production dependency audit
  - backend fast regression / migration / security gates
- `main` 不再要求 legacy `frontend/` 的测试、i18n 或构建门禁。

### 日志
- 后端日志为结构化 JSON，包含：
  - `requestId`
  - `event`
  - `module`
  - `userId`
  - `sessionId`
- 排查请求链时，优先按 `requestId` 聚合：
  - `docker compose logs backend | rg '"requestId":"'`

### 指标
- 导出 Prometheus 文本：
  - `curl -s -H "Authorization: Bearer <admin-access-token>" http://127.0.0.1:8080/actuator/prometheus`
- 关键指标：
  - `mmmail_api_requests_total`
  - `mmmail_api_requests_failed_total`
  - `mmmail_errors_events_total`
  - `mmmail_jobs_executions_total`

## 3. 常见故障排查
### 系统健康页显示 `DOWN` 或 `DEGRADED`
- 先看 `components` 区块中哪个组件异常。
- `redis=DOWN`：
  - 检查 `SPRING_REDIS_HOST`、`SPRING_REDIS_PORT`、`SPRING_REDIS_PASSWORD`
- `db=DOWN`：
  - 先执行 `./scripts/db-upgrade.sh .env info`
  - 再检查 `SPRING_DATASOURCE_*` 配置和 MySQL 连接性

### `/actuator/prometheus` 返回 `403`
- 该接口仅管理员可访问。
- 先重新登录管理员，再确认请求带 `Authorization: Bearer <token>`。

### Swagger UI 或 OpenAPI 无法打开
- 先确认后端容器健康：
  - `curl -sf http://127.0.0.1:8080/actuator/health`
- 再检查部署时的 API origin 与反向代理路径是否一致。

### Frontend 页面可打开但 API 异常
- 先检查 `VITE_API_BASE_URL`。
- 再看 `docker compose logs backend`。
- 最后确认浏览器请求是否命中当前 `main` 的后端环境。

### Public share 页面异常
- Mail：`/share/mail/{token}`
- Drive：`/share/drive/{token}`
- Pass：`/share/pass/{token}`
- 若返回 `403` 或 `404`：
  - 检查反向代理是否放行 `/api/v1/public/**`
  - 检查安全配置与公开路由映射是否仍保持公开访问

## 4. 升级 / 回滚 / 恢复
- 升级前：
  - `./scripts/db-backup.sh .env`
  - `./scripts/db-upgrade.sh .env info`
- 执行升级：
  - `./scripts/db-upgrade.sh .env upgrade`
- 前滚修复：
  - `./scripts/db-upgrade.sh .env repair`
  - `./scripts/db-upgrade.sh .env upgrade`
- 恢复：
  - `./scripts/db-restore.sh .env <backup-dir>`
- 回滚：
  - `./scripts/db-rollback.sh .env <backup-dir>`

## 5. 发布前运维检查
- 本地：
  - `bash scripts/validate-local.sh`
- 手动确认：
  - 管理员可打开 `/settings/system-health`
  - `/actuator/prometheus` 可导出核心指标
  - `/boundary` 与 `docs/release/v2-support-boundaries.md` 一致
  - 公开基线与 `docs/open-source/module-maturity-matrix.md` 一致
