# Community Edition v1.6.1 安装说明

**版本**: `v1.6.1`
**日期**: `2026-04-15`

## 当前边界
- 当前公开基线为 `Community Edition v1.6.1`，对外交付重点是清晰的自托管启动路径、真实能力边界和可验证的默认门禁。
- 当前自托管运行模型仍然是 `Nuxt Web + 单个 Spring Boot 后端进程 + MySQL / Redis`。
- 标准模式可以包含 `Nacos`，但这不代表仓库已交付真正的 `Spring Cloud` 微服务网格；最小模式只是把本地依赖收敛到更轻量的采用路径。
- 浏览器侧已经交付：
  - `/suite` 的 `Overview / Plans / Billing / Operations / Boundary` 分区视图
  - `/suite` 总览中的 `Mail → Calendar → Drive → Pass` 主线协作链路
  - `Pass` 默认导航可见 `Beta` 入口
  - `/labs` 默认 curated catalog（`Authenticator / SimpleLogin / Standard Notes`）
  - `PWA` manifest、Service Worker 注册与安装入口
  - `Mail E2EE` 当前闭环（key profile、READY 内部路由正文加密、草稿加密、附件加密、详情本地解密、密钥恢复）
  - `Mail` 外部密码保护安全投递（浏览器内加密正文 / 附件、草稿恢复、公开页本地解密下载）
  - `Drive E2EE foundation` 与单文件 `readable-share` E2EE foundation
  - `Web Push`
  - `SMTP outbound adapter`
  - `Calendar internal invitation orchestration`
  - `Pass Beta readiness`
  - 浏览器内 `adoption guide`
  - 关键公开面运行时 a11y 自动化门禁
- 仍未交付：
  - `SMTP inbound / IMAP / Bridge`
  - 完整 MIME 级外部 E2EE 互通
  - 真正零知识邮件架构
  - 原生客户端
  - `VPN / Meet / Wallet / Lumo` 的真实引擎

## 前置条件
- `Docker` + `Docker Compose v2`
- 至少 `4 CPU / 8 GB RAM`
- 标准模式端口：`3001`、`8080`、`3306`、`6379`、`8848`
- 最小模式端口：`3001`、`8080`、`3306`、`6379`

## 1. 准备运行时环境
1. 复制模板：
   - `cp .env.example .env`
2. 编辑 `.env`，至少替换以下占位值：
   - 两种模式都必须替换：
     - `MMMAIL_JWT_SECRET`
     - `SPRING_DATASOURCE_PASSWORD`
     - `SPRING_REDIS_PASSWORD`
     - `MYSQL_ROOT_PASSWORD`

## 2. 推荐首次采用：最小模式
- 确认 `.env` 中 `MMMAIL_NACOS_ENABLED=false`
- 最小模式首次启动不要求填写 `NACOS_USERNAME` / `NACOS_PASSWORD`
- 最小模式把本地依赖收敛为 `Nuxt Web + 单个 Spring Boot 后端进程 + MySQL / Redis`

## 3. 启动前校验
- 执行：
  - `./scripts/validate-runtime-env.sh .env`

校验通过后才允许继续启动；若脚本报 placeholder / missing，必须先修正 `.env`。当 `MMMAIL_NACOS_ENABLED=false` 时，脚本会显式跳过 Nacos 凭据要求。

## 4. 启动 Compose
- 最小模式：
  - `docker compose --env-file .env -f docker-compose.minimal.yml up -d --build`
- 查看状态：
  - `docker compose -f docker-compose.minimal.yml ps`
- 查看日志：
  - `docker compose -f docker-compose.minimal.yml logs -f backend`
  - `docker compose -f docker-compose.minimal.yml logs -f frontend`

## 5. 如需标准模式再启用 Nacos
- 设置 `.env` 中 `MMMAIL_NACOS_ENABLED=true`
- 额外替换：
  - `NACOS_USERNAME`
  - `NACOS_PASSWORD`
- 对 isolated local 标准模式，`NACOS_USERNAME` / `NACOS_PASSWORD` 可设置为本地默认 `nacos`；不要把该值回写到仓库模板。
- 启动：
  - `docker compose --env-file .env up -d --build`
- 查看状态：
  - `docker compose ps`
- 查看日志：
  - `docker compose logs -f backend`
  - `docker compose logs -f frontend`

## 6. 验证服务
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
  - `http://127.0.0.1:3001/self-hosted/adoption.html`
  - `http://127.0.0.1:3001/self-hosted/architecture.html`
- 数据迁移状态：
  - `./scripts/db-upgrade.sh .env info`

## 6.1 设置与边界页采用准备度检查
- 公开边界入口：
  - `http://127.0.0.1:3001/boundary`
- 使用管理员账号登录后访问：
  - `http://127.0.0.1:3001/settings`
  - `http://127.0.0.1:3001/suite?section=boundary`
  - `http://127.0.0.1:3001/labs`
  - `http://127.0.0.1:3001/pass`
- 确认以下面板 / 页面状态符合预期：
  - `Mail E2EE foundation`
  - `Adoption readiness`
  - `PWA readiness`
  - 公开 `/boundary` 页面可直接查看边界说明
  - 登录后的 `/suite?section=boundary` 仍显示 `Release boundary map`
  - `Suite Overview` 可见主线协作链路
  - `Pass` 已出现在默认导航
  - `Labs` 默认 catalog 只展示 `Authenticator / SimpleLogin / Standard Notes`
- `Adoption readiness` 面板会直接暴露：
  - 浏览器内 `adoption guide`
  - 浏览器内 `self-hosted architecture guide`
  - 内置 `API quick page`
  - 后端 `Swagger UI`
  - 后端 `OpenAPI JSON`
  - 内置的自托管安装说明与 Runbook 快速页

## 7. 停止与清理
- 停止：
  - 标准模式：`docker compose down`
  - 最小模式：`docker compose -f docker-compose.minimal.yml down`
- 连同卷一起清理：
  - 标准模式：`docker compose down -v`
  - 最小模式：`docker compose -f docker-compose.minimal.yml down -v`

## 8. 常见问题
- `validate-runtime-env.sh` 失败：
  - `.env` 仍保留 `replace-with-*` 占位值。
- 最小模式仍要求 Nacos 凭据：
  - 先确认 `.env` 里的 `MMMAIL_NACOS_ENABLED=false`，再重新运行 `./scripts/validate-runtime-env.sh .env`。
- Backend 无法连接 MySQL：
  - 检查 `SPRING_DATASOURCE_PASSWORD` 与 `MYSQL_ROOT_PASSWORD` 是否已正确设置。
- Frontend 页面打开但 API `403 / 500`：
  - 先看 `docker compose logs backend`，再检查 `MMMAIL_JWT_SECRET` 与数据库初始化日志。
- `Release boundary map` 与 `/labs` 页面不符合预期：
  - 先确认构建产物来自当前 `v1.6.1` 发布代码
  - 再确认前端静态资源没有被旧 CDN / 反向代理缓存污染
- 外部密码保护加密邮件无法打开：
  - 先确认 SMTP 通知邮件中的 secure link 使用了正确 `public base URL`
  - 再确认公开页面请求 `/api/v1/public/mail/{token}` 与 `/api/v1/public/mail/{token}/access` 未被反向代理拦截
