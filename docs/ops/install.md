# Community Edition v1.4 安装说明

**版本**: `v1.4-mainline`  
**日期**: `2026-04-07`  
**作者**: `Codex`

## 当前边界
- 当前版本是 `Community Edition v1.4` 主线，不承诺 `SMTP inbound / IMAP / Bridge`、零知识架构、外部加密附件 / 外部加密草稿、完整 MIME 外部 E2EE 兼容或 Hosted 自动化 onboarding。
- 浏览器侧已经交付：
  - `PWA` manifest、Service Worker 注册与安装入口
  - `Mail E2EE` 当前闭环（key profile、READY 内部路由正文加密、草稿加密、附件加密、详情本地解密、密钥恢复）
  - `Mail` 外部密码保护加密投递（body-only secure link）
  - `Drive E2EE foundation` 与单文件 `readable-share` E2EE foundation
  - `Web Push`
  - `SMTP outbound adapter`
  - `Calendar internal invitation orchestration`
  - `Pass Beta readiness`
- 仍未交付：
  - `SMTP inbound / IMAP / Bridge`
  - 外部加密附件 / 外部加密草稿 / 完整 MIME 外部 E2EE
  - 真正零知识邮件架构
  - 原生客户端

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
- Frontend PWA manifest：
  - `curl -sf http://127.0.0.1:3001/manifest.webmanifest`
- Backend health：
  - `curl -sf http://127.0.0.1:8080/actuator/health`
- Backend OpenAPI / Swagger UI：
  - `http://127.0.0.1:8080/swagger-ui.html`
  - `curl -sf http://127.0.0.1:8080/v3/api-docs`
- Frontend bundled API quick page：
  - `http://127.0.0.1:3001/self-hosted/api.html`
- 数据迁移状态：
  - `./scripts/db-upgrade.sh .env info`

## 4.1 设置页采用准备度检查
- 使用管理员账号登录后访问：
  - `http://127.0.0.1:3001/settings`
- 确认以下面板出现且状态符合预期：
  - `Mail E2EE foundation`
  - `Adoption readiness`
  - `PWA readiness`
- `Adoption readiness` 面板会直接暴露：
  - 内置 `API quick page`
  - 后端 `Swagger UI`
  - 后端 `OpenAPI JSON`
  - 内置的自托管安装说明与 Runbook 快速页

## 4.2 RC1 冷启动证据
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
- 外部密码保护加密邮件无法打开：
  - 先确认 SMTP 通知邮件中的 secure link 使用了正确 `public base URL`
  - 再确认公开页面请求 `/api/v1/public/mail/{token}` 与 `/api/v1/public/mail/{token}/access` 未被反向代理拦截
