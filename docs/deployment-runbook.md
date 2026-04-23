# MMMail v2.0.3 Deployment Runbook

**版本**: `v2.0.3`
**日期**: `2026-04-23`
**状态**: `released candidate`

## 1. 目的
- 记录 `main` 切到 v2-only 之后的部署基线。
- 统一 `frontend-v2`、Compose、自托管门禁与发布文档口径。
- 更完整的安装、升级、备份恢复、运维排障请参考：
  - `docs/ops/install.md`
  - `docs/ops/upgrade.md`
  - `docs/ops/backup-restore.md`
  - `docs/ops/runbook.md`

## 2. 适用部署基线
- 当前 shipped baseline：`main` / `v2.0.3`
- 当前默认自托管运行模型：`frontend-v2 Web + 单个 Spring Boot 后端进程 + MySQL / Redis`
- 标准模式可额外启用 `Nacos`
- archive 分支 `archive/v2-only-pre-cleanup-20260423` 仅用于保留清理前仓库状态，不参与当前发布门禁

## 3. 部署前检查
1. 校验运行时环境
   - `./scripts/validate-runtime-env.sh .env`
2. 执行本地门禁
   - `bash scripts/validate-local.sh`
3. 如需数据库变更前保护，先执行备份
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
- Swagger UI：`http://127.0.0.1:8080/swagger-ui.html`

### 5.2 主线验收
- `Mail / Calendar / Drive / Suite Shell / Business / Organizations / Settings / Security` 保持当前公开主线
- `Pass / Docs / Sheets` 仍按 `Beta` 口径暴露
- `Collaboration / Command Center / Notifications / Labs` 保持 `Preview`
- `frontend-v2` tests、contract regression、typecheck 与 audit 门禁全部通过
- backend migration、security 与 capability 回归全部通过

## 6. 故障处理
- 应用健康异常、指标、后台任务、权限边界等运行态排查，参考 `docs/ops/runbook.md`
- 数据库升级失败时，优先按 `docs/ops/upgrade.md` 执行前滚修复
- 需要恢复时，按 `docs/ops/backup-restore.md` 执行恢复或回滚

## 7. 发布工件对齐
- Release notes source of truth: `docs/release/v2.0.3-release-notes.md`
- Support boundaries: `docs/release/v2-support-boundaries.md`
- Module maturity: `docs/open-source/module-maturity-matrix.md`
