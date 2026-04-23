# MMMail v2.0.0 / v2.0.1 / v2.0.2 Deployment Runbook

**版本**: `v2.0.0` baseline / `v2.0.1` hardening / `v2.0.2` closure
**日期**: `2026-04-22`
**状态**: `v2.0.0 released / v2.0.1 candidate / v2.0.2 candidate`

## 1. 目的
- 这份 runbook 记录 `v2.0.0` 已发布后的部署基线，以及 `v2.0.1`、`v2.0.2` 在同一 shipped boundary 内的硬化验收要求。
- `v2.0.0` 已经在 `main` 发布，并已存在 Git tag / GitHub Release；`v2.0.1` 与 `v2.0.2` 继续沿用同一发布边界，不引入新的预发布流程。
- 更完整的安装、升级、备份恢复、运维排障请分别参考：
  - `docs/ops/install.md`
  - `docs/ops/upgrade.md`
  - `docs/ops/backup-restore.md`
  - `docs/ops/runbook.md`
  - `docs/ops/team-enablement.md`

## 2. 适用部署基线
- 当前 shipped baseline：`main` / tag `v2.0.0`，`v2.0.1` 与 `v2.0.2` 作为同边界 hardening follow-up
- 当前默认自托管运行模型：`Nuxt Web + 单个 Spring Boot 后端进程 + MySQL / Redis`
- `frontend-v2` 已纳入本地与 CI 发布门禁，`v2.0.1` 进一步将前端契约套件与后端 capability/header/observability 契约回归纳入显式门禁，`v2.0.2` 补充 route/workspace aggregation/billing readiness closure regression

## 3. 部署前检查
1. 校验运行时环境
   - `./scripts/validate-runtime-env.sh .env`
2. 执行本地门禁
   - `bash scripts/validate-local.sh`
3. 如需在 CI 环境复核，执行
   - `bash scripts/validate-ci.sh`
4. 如需数据库变更前保护，先执行备份
   - `./scripts/db-backup.sh .env`

## 4. 部署执行
### 4.1 最小模式
- `docker compose --env-file .env -f docker-compose.minimal.yml up -d --build`

### 4.2 标准模式
- `docker compose --env-file .env up -d --build`

### 4.3 数据库升级
- 查看状态：`./scripts/db-upgrade.sh .env info`
- 执行升级：`./scripts/db-upgrade.sh .env upgrade`
- 如需前滚修复：`./scripts/db-upgrade.sh .env repair`

## 5. 发布后验证
### 5.1 服务与页面
- Frontend：`http://127.0.0.1:3001`
- Backend health：`http://127.0.0.1:8080/actuator/health`
- Boundary page：`http://127.0.0.1:3001/boundary`
- Labs catalog：`http://127.0.0.1:3001/labs`
- Swagger UI：`http://127.0.0.1:8080/swagger-ui.html`

### 5.2 v2.0.0 重点验收
- `Mail → Calendar → Drive → Pass` 主线协作链路可见
- `Pass` 继续以默认导航中的 `Beta` 入口出现
- `/labs/:moduleKey` 仍承载 preview 模块，不误标成 GA
- legacy compatibility redirects 保持可用
- `frontend-v2` 测试与 typecheck 门禁已通过

### 5.3 v2.0.1 硬化验收补充
- `frontend-v2` 设置页支持 scope-aware panel query，至少核对 `privacy-telemetry`、`system-health`、`integrations`
- `frontend-v2` system health 面板可读取当前 shipped `/api/v1/system/health` 管理端健康概览
- public-share、workspace aggregation、platform、AI/MCP capability 合同回归通过
- 本地与 CI 快速门禁均显式执行 `frontend-v2` contract regression 与后端 capability/header/observability contract regression

## 5.4 v2.0.2 硬化验收补充
- `frontend-v2` route contracts显式覆盖 named redirects 与 `/folders/:folderId` -> `/folders/:id` 的 same-shape compatibility 事实
- `frontend-v2` 的 `CollaborationView` 与 `NotificationsView` 都通过当前 `requestHeaders` 读取 `/api/v2/workspace/aggregation`
- `BillingReadinessIntegrationTest` 已进入本地与 CI 的 backend fast contract regression

## 5.5 backend v2 remaining hardening checks
- tenant/scope foundation kernel is shared through `mmmail-foundation` instead of duplicated raw header strings
- token-hash public-share contract covers mail, pass, and drive without changing the shipped public routes
- module kernel extraction keeps `identity`, `org-governance`, `workspace`, and `billing` from remaining marker-only during release sign-off

## 5.6 frontend-v2 runtime parity checks
- frontend-v2 public-share surfaces read live mail/drive/pass payloads from route tokens instead of placeholder titles
- frontend-v2 mail, calendar, drive, and pass routes read authenticated workspace APIs
- frontend-v2 docs and sheets routes read route-id-driven runtime data before release sign-off

## 6. 故障处理
- 应用健康异常、指标、后台任务、权限边界等运行态排查，参考 `docs/ops/runbook.md`
- 数据库升级失败时，优先按 `docs/ops/upgrade.md` 执行前滚修复
- 需要恢复时，按 `docs/ops/backup-restore.md` 执行恢复或回滚

## 7. 发布工件对齐
- Release notes source of truth: `docs/release/v2.0.0-release-notes.md`、`docs/release/v2.0.1-release-notes.md`、`docs/release/v2.0.2-release-notes.md`
- GitHub Release: `https://github.com/IMG-LTD/MMMail/releases/tag/v2.0.0`
- 若发布说明出现文档漂移，优先更新对应 release notes 文档；`v2.0.1` 与 `v2.0.2` 均不改变 `v2.0.0` tag 与 shipped boundary claim
