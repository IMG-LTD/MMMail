# Community Edition v1.6.1 备份与恢复

**版本**: `v1.6.1`
**日期**: `2026-04-15`

## 备份范围
- MySQL 业务库：`database.sql`
- Drive 持久化目录：`drive-data.tar.gz`
- 备份元数据：`manifest.txt`

## 前提
- `.env` 中必须包含：
  - `SPRING_DATASOURCE_URL`
  - `SPRING_DATASOURCE_USERNAME`
  - `SPRING_DATASOURCE_PASSWORD`
  - `MMMAIL_DRIVE_STORAGE_ROOT`
- 若宿主机未安装 `mysql` / `mysqldump`，脚本会尝试使用 Docker 容器客户端。

## 创建备份
- 默认输出到 `backups/<timestamp>`：
  - `./scripts/db-backup.sh .env`
- 指定输出目录：
  - `./scripts/db-backup.sh .env ./backups/pre-upgrade`

## 恢复
- 完整恢复数据库与 Drive 数据：
  - `./scripts/db-restore.sh .env ./backups/pre-upgrade`

## 回滚
- 升级失败时使用：
  - `./scripts/db-rollback.sh .env ./backups/pre-upgrade`

## 恢复后验证
- 检查备份 manifest 中的 schema 版本：
  - `cat ./backups/pre-upgrade/manifest.txt`
- 检查数据库对象恢复：
  - `./scripts/db-upgrade.sh .env info`
- 检查 Drive 文件恢复：
  - 对比 `MMMAIL_DRIVE_STORAGE_ROOT` 中关键文件
- 如需补充运维排查，参考：
  - `docs/ops/runbook.md`
  - `docs/architecture/database-migration-strategy.md`
