# MMMail v2.0.0 Deployment Runbook

**版本**: `v2.0.0`
**日期**: `2026-04-21`
**状态**: `released`

## 1. 目的
- 这份 runbook 记录 `v2.0.0` 已发布后的部署与验收基线。
- `v2.0.0` 已经在 `main` 发布，并已存在 Git tag / GitHub Release；本手册不再包含合并 `release/2.0.0`、创建 `v2.0.0` tag、发布 GitHub Release 等预发布动作。
- 更完整的安装、升级、备份恢复、运维排障请分别参考：
  - `docs/ops/install.md`
  - `docs/ops/upgrade.md`
  - `docs/ops/backup-restore.md`
  - `docs/ops/runbook.md`
  - `docs/ops/team-enablement.md`

## 2. 适用部署基线
- 当前 shipped baseline：`main` / tag `v2.0.0`
- 当前默认自托管运行模型：`Nuxt Web + 单个 Spring Boot 后端进程 + MySQL / Redis`
- `frontend-v2` 已纳入本地与 CI 发布门禁

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

## 6. 故障处理
- 应用健康异常、指标、后台任务、权限边界等运行态排查，参考 `docs/ops/runbook.md`
- 数据库升级失败时，优先按 `docs/ops/upgrade.md` 执行前滚修复
- 需要恢复时，按 `docs/ops/backup-restore.md` 执行恢复或回滚

## 7. 发布工件对齐
- Release notes source of truth: `docs/release/v2.0.0-release-notes.md`
- GitHub Release: `https://github.com/IMG-LTD/MMMail/releases/tag/v2.0.0`
- 若发布说明出现文档漂移，只更新 release notes 文档与现有 GitHub Release notes，不变更 tag
