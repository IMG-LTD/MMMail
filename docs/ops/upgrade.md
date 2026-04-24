# MMMail v2 Mainline 升级说明

**版本**: `v2.0.4`
**日期**: `2026-04-23`

## 适用范围
- 适用于当前 `main` / `v2.0.4` 公开基线的数据库 schema 升级。
- 当前默认数据库为 `MySQL 8`。
- 当前默认运行模型为 `frontend-v2 Web + 单个 Spring Boot 后端进程 + MySQL / Redis`。

## 升级前提
- 已准备可写 `.env`
- `.env` 已通过：
  - `./scripts/validate-runtime-env.sh .env`
- 目标环境可以访问数据库
- 升级前必须先做备份：
  - `./scripts/db-backup.sh .env`

## 升级流程
1. 查看当前迁移状态：
   - `./scripts/db-upgrade.sh .env info`
2. 校验迁移一致性：
   - `./scripts/db-upgrade.sh .env validate`
3. 执行升级：
   - `./scripts/db-upgrade.sh .env upgrade`
4. 升级后再次检查：
   - `./scripts/db-upgrade.sh .env info`

## 失败处理
### 优先策略：前滚修复
- 若只是单个迁移失败，优先修正迁移问题后重新执行：
  - `./scripts/db-upgrade.sh .env repair`
  - `./scripts/db-upgrade.sh .env upgrade`

### 回滚策略：备份恢复
- 当前不支持自动 down migration。
- 若无法前滚修复，使用升级前备份执行完整恢复：
  - `./scripts/db-rollback.sh .env <backup-dir>`

## 当前 schema 基线
- 当前 `v2.0.4` 公开基线对应最新 schema 版本 `15`。
- `system_release_metadata.schema_version` 与 `flyway_schema_history` 中的最新成功版本应保持一致。

## 验证方式
- `flyway_schema_history` 中存在最新成功版本
- `system_release_metadata` 的 `schema_version` 为 `15`
- 应用健康检查恢复：
  - `curl -sf http://127.0.0.1:8080/actuator/health`
- 如需补充运维排查，参考：
  - `docs/ops/runbook.md`
  - `docs/architecture/database-migration-strategy.md`
