# Community Edition v1.0 安装说明

**版本**: `v1.0-draft`  
**日期**: `2026-03-13`  
**作者**: `Codex`

## 前置条件
- `Docker` + `Docker Compose v2`
- 至少 `4 CPU / 8 GB RAM`
- 可用端口：`3001`、`8080`、`3306`、`6379`、`8848`

## 1. 准备运行时环境
1. 复制模板：
   - `cp .env.example .env`
2. 编辑 `.env`，至少替换以下占位值：
   - `MMMAIL_JWT_SECRET`
   - `SPRING_DATASOURCE_PASSWORD`
   - `SPRING_REDIS_PASSWORD`
   - `NACOS_USERNAME`
   - `NACOS_PASSWORD`
   - `MYSQL_ROOT_PASSWORD`
3. 对 isolated local 环境，`NACOS_USERNAME` / `NACOS_PASSWORD` 可设置为本地默认 `nacos`；不要把该值回写到仓库模板。

## 2. 启动前校验
- 执行：
  - `./scripts/validate-runtime-env.sh .env`

校验通过后才允许继续启动；若脚本报 placeholder/missing，必须先修正 `.env`。

## 3. 启动 Compose
- 构建并启动：
  - `docker compose --env-file .env up -d --build`
- 查看状态：
  - `docker compose ps`
- 查看日志：
  - `docker compose logs -f backend`
  - `docker compose logs -f frontend`

## 4. 验证服务
- Frontend：
  - `http://127.0.0.1:3001`
- Backend health：
  - `curl -sf http://127.0.0.1:8080/actuator/health`
- 数据迁移状态：
  - `./scripts/db-upgrade.sh .env info`

## 4.1 RC1 冷启动证据
- 本机证据入口：
  - `bash scripts/validate-rc1-local.sh`
- Docker-capable 环境证据入口：
  - `bash scripts/validate-rc1-container.sh`
- 外部执行说明：
  - `docs/release/external-ci-handoff.md`

## 5. 停止与清理
- 停止：
  - `docker compose down`
- 连同卷一起清理：
  - `docker compose down -v`

## 6. 常见问题
- `validate-runtime-env.sh` 失败：
  - `.env` 仍保留 `replace-with-*` 占位值。
- Backend 无法连接 MySQL：
  - 检查 `SPRING_DATASOURCE_PASSWORD` 与 `MYSQL_ROOT_PASSWORD` 是否已正确设置。
- Frontend 页面打开但 API 403/500：
  - 先看 `docker compose logs backend`，再检查 `MMMAIL_JWT_SECRET` 与数据库初始化日志。
