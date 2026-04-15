# Community Edition v1.6.1 数据迁移策略

**版本**: `v1.6.1`  
**日期**: `2026-04-15`  

## 目标
- 将历史 `schema.sql + data.sql + Spring SQL init` 模式收敛到 Flyway 版本化迁移。
- 统一新部署与存量库的升级入口。
- 明确回滚策略：不提供自动 down migration，使用升级前备份 + 完整恢复。

## 迁移机制
- 引擎：`Flyway 10`
- 迁移位置：
  - Java 基线迁移：`backend/mmmail-server/src/main/java/db/migration`
  - SQL 增量迁移：`backend/mmmail-server/src/main/resources/db/migration`
- 应用默认配置：
  - `spring.flyway.enabled=true`
  - `spring.flyway.baseline-on-migrate=true`
  - `spring.flyway.baseline-version=1`
  - `SPRING_SQL_INIT_MODE=never`

## 基线策略
### 新部署
- 空数据库会从 `V1__baseline_schema` 开始，并继续执行当前公开基线之前的全部增量迁移。
- 当前 `Community Edition v1.6.1` 公开基线对应最新 schema 版本 `13`。

### 存量库
- 对已存在业务表、但没有 `flyway_schema_history` 的数据库：
  - Flyway 先写入 `BASELINE 1`
  - 然后继续执行 `V2+` 的幂等迁移
- 这样可以同时兼容：
  - 已执行过 `schema.sql` 但未执行 `data.sql` 的旧库
  - 已执行过 `schema.sql + data.sql` 的旧库

## 升级入口
- 统一脚本：`scripts/db-upgrade.sh`
- 支持命令：
  - `upgrade`
  - `info`
  - `validate`
  - `migrate`
  - `repair`

## 回滚策略
- 不支持 Flyway down migration。
- 当前回滚策略固定为：
  1. 升级前执行 `scripts/db-backup.sh`
  2. 若升级失败且不能前滚修复，执行 `scripts/db-rollback.sh`
  3. `db-rollback.sh` 会显式调用 `scripts/db-restore.sh`

## 备份覆盖面
- MySQL 业务库
- Drive 文件持久化目录（`MMMAIL_DRIVE_STORAGE_ROOT`）

以下数据当前不纳入默认强制备份：
- Redis 会话/限流缓存（可重建）
- Nacos 本地元数据（开发/单机场景可重建）

## 验证链
- `scripts/validate-batch3.sh`
- `backend/mmmail-server/src/test/java/com/mmmail/server/FlywayMigrationIntegrationTest.java`
- `backend/mmmail-server/src/test/java/com/mmmail/server/BackupRestoreWorkflowIntegrationTest.java`
- `backend/mmmail-server/src/test/java/com/mmmail/server/MigrationCliWorkflowIntegrationTest.java`
