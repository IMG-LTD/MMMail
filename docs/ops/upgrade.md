# Community Edition v1.0 升级说明

**版本**: `v1.0-draft`  
**日期**: `2026-03-13`  
**作者**: `Codex`

## 适用范围
- 适用于 Community Edition v1.0 的数据库 schema 升级。
- 当前默认数据库为 MySQL 8。

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
- 首发不支持自动 down migration。
- 若无法前滚修复，使用升级前备份执行完整恢复：
  - `./scripts/db-rollback.sh .env <backup-dir>`

## 验证方式
- `flyway_schema_history` 中存在最新成功版本
- `system_release_metadata` 的 `schema_version` 为当前版本
- 应用健康检查恢复：
  - `curl -sf http://127.0.0.1:8080/actuator/health`

## RC1 证据入口
- 本机统一证据：
  - `bash scripts/validate-rc1-local.sh`
- Docker-capable 升级 / 回滚 / 恢复证据：
  - `bash scripts/validate-rc1-container.sh`
