# MMMail v2.1.2 Deployment Runbook

**版本**: `v2.1.2`
**日期**: `2026-05-16`
**状态**: `release candidate`

## 1. 目的
- 记录 `main` 切到 v2.1.2 之后的部署基线。
- 统一 `frontend-v2`、Compose、自托管门禁与发布文档口径。
- First-time install source of truth: `docs/ops/install.md`。
- 首次安装路径选择（一键安装、Docker 手动安装、裸机手动安装、本地体验 / 开发）以 `docs/ops/install.md` 为准；本 Runbook 从操作者已经选定安装路径之后开始，聚焦部署执行、验证、升级、备份、恢复与事故处理步骤。
- 更完整的升级、备份恢复、运行态排障请参考：
  - `docs/ops/upgrade.md`
  - `docs/ops/backup-restore.md`
  - `docs/ops/runbook.md`

## 2. 适用部署基线
- 当前 shipped baseline：`main` / `v2.1.2`
- 当前默认自托管运行模型：`frontend-v2 Web + 单个 Spring Boot 后端进程 + MySQL / Redis`
- 标准模式可额外启用 `Nacos`
- archive 分支 `archive/v2-only-pre-cleanup-20260423` 仅用于保留清理前仓库状态，不参与当前发布门禁

## 3. 部署前检查
1. 确认已经按 `docs/ops/install.md` 选定安装路径并准备 `.env`
2. 校验运行时环境
   - `./scripts/validate-runtime-env.sh .env`
3. 执行本地门禁
   - `bash scripts/validate-local.sh`
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
- Swagger UI：`http://127.0.0.1:8080/swagger-ui.html`

### 5.2 主线验收
- `Mail / Calendar / Drive / Suite Shell / Business / Organizations / Settings / Security` 保持当前公开主线
- `Pass / Docs / Sheets` 仍按 `Beta` 口径暴露
- `Collaboration / Command Center / Notifications / Labs` 保持 `Preview`
- `frontend-v2` tests、contract regression、typecheck 与 audit 门禁全部通过
- backend migration、security 与 capability 回归全部通过
- 发布门禁必须覆盖 `token-hash public-share contract`
- 发布门禁必须覆盖 `tenant/scope foundation kernel`
- 发布门禁必须覆盖 `module kernel extraction`

### 5.3 v2.1.2 WebSocket 网关压测
- 目标：`/ws/notifications` 1000 连接稳定 30 分钟，服务端 CPU < 30%，JVM 内存 < 1 GB。
- 前置：后端真实运行，准备 5 分钟内签发的 access token，并确保 `/actuator/prometheus` 可由管理员 token 或内网访问；管理员 token 有效期必须覆盖完整 `WS_DURATION_MS`，本地 30 分钟压测可临时设置 `MMMAIL_JWT_EXPIRE_MINUTES=60`。
- 命令：
  ```bash
  WS_BASE_URL=ws://127.0.0.1:8080 \
  WS_TOKEN='<短期 access token>' \
  WS_PROMETHEUS_URL=http://127.0.0.1:8080/actuator/prometheus \
  WS_PROMETHEUS_BEARER_TOKEN='<管理员 access token>' \
  WS_CONNECTIONS=1000 \
  WS_DURATION_MS=1800000 \
  node ops/ws-gateway-load-test.mjs
  ```
- 通过标准：脚本退出码为 0，输出 JSON 中 `errors=0`、`throttles=0`、`closedUnexpected=0`，Grafana 面板 `MMMail WebSocket Gateway` 同步显示连接数、消息吞吐、消息延迟、断开原因、协同房间数与通知扇出延迟。

### 5.4 v2.1.2 敏感写操作限流
- 登录、邮件外发、Web Push 测试与命令运行均接入后端统一限流器，超限返回 `429` 与 `RATE_LIMITED` 响应。
- 可调环境变量：
  - `MMMAIL_SECURITY_MAIL_SEND_RATE_LIMIT_WINDOW_SECONDS` / `MMMAIL_SECURITY_MAIL_SEND_RATE_LIMIT_MAX_EVENTS`
  - `MMMAIL_SECURITY_WEB_PUSH_TEST_RATE_LIMIT_WINDOW_SECONDS` / `MMMAIL_SECURITY_WEB_PUSH_TEST_RATE_LIMIT_MAX_EVENTS`
  - `MMMAIL_SECURITY_COMMAND_RUN_RATE_LIMIT_WINDOW_SECONDS` / `MMMAIL_SECURITY_COMMAND_RUN_RATE_LIMIT_MAX_EVENTS`

### 5.5 v2.1.2 WebSocket 运行时变量
- `MMMAIL_WEBSOCKET_AFFINITY_COOKIE_NAME`：WebSocket 粘性会话 cookie 名。
- `MMMAIL_WEBSOCKET_AFFINITY_NODE_ID`：当前后端节点标识。
- `MMMAIL_WEBSOCKET_AFFINITY_COOKIE_SECURE`：生产 HTTPS 部署应保持 `true`。
- `MMMAIL_WEBSOCKET_AFFINITY_COOKIE_MAX_AGE_SECONDS`：粘性会话 cookie 生命周期。
- `MMMAIL_WEBSOCKET_SESSION_MAX_IDLE_MS`：空闲会话关闭阈值。
- `MMMAIL_WEBSOCKET_CONNECTION_MAX_ACTIVE`：单节点最大活动连接数。
- `MMMAIL_WEBSOCKET_CONNECTION_RETRY_AFTER_MS`：连接超限后的重试建议。
- `MMMAIL_WEBSOCKET_SUBSCRIPTION_MAX_CHANNELS_PER_SESSION`：单会话最大订阅频道数。
- `MMMAIL_WEBSOCKET_SUBSCRIPTION_RETRY_AFTER_MS`：订阅超限后的重试建议。
- `MMMAIL_WEBSOCKET_RATE_LIMIT_WINDOW_SECONDS`：消息限流窗口。
- `MMMAIL_WEBSOCKET_RATE_LIMIT_MAX_MESSAGES_PER_WINDOW`：窗口内最大消息数。
- `MMMAIL_WEBSOCKET_RATE_LIMIT_RETRY_AFTER_MS`：消息限流后的重试建议。

### 5.6 v2.1.2 外部集成变量
- `MMMAIL_MAIL_EXTERNAL_ACCOUNT_SECRET`：IMAP / SMTP 外部账号凭据加密密钥，生产必须配置为 32 位以上随机值。
- `MMMAIL_WEB_PUSH_VAPID_SUBJECT`：Web Push VAPID subject。
- `MMMAIL_WEB_PUSH_VAPID_PUBLIC_KEY`：Web Push VAPID 公钥。
- `MMMAIL_WEB_PUSH_VAPID_PRIVATE_KEY`：Web Push VAPID 私钥。
- `MMMAIL_FEATURE_FLAGS_WATCH_INTERVAL_MS`：feature_flag 表 watch 刷新间隔，默认 `5000`。

### 5.7 v2.1.2 dev seed 变量
- dev seed 仅在 `SPRING_PROFILES_ACTIVE=dev` 时运行，seed 执行失败只记录 warn，不阻断应用启动。
- `MMMAIL_DEV_SEED_ENABLED`：总开关。
- `MMMAIL_DEV_SEED_WALLET`：钱包 seed。
- `MMMAIL_DEV_SEED_MEET`：会议 seed。
- `MMMAIL_DEV_SEED_COMMUNITY`：社区 seed。
- `MMMAIL_DEV_SEED_SEARCH_INDEX`：搜索索引 seed。
- `MMMAIL_DEV_SEED_DOMAIN`：自定义域名 seed。
- `MMMAIL_DEV_SEED_WEBPUSH`：Web Push seed，默认关闭，启用前必须替换本地占位订阅。

## 6. 故障处理
- 应用健康异常、指标、后台任务、权限边界等运行态排查，参考 `docs/ops/runbook.md`
- 数据库升级失败时，优先按 `docs/ops/upgrade.md` 执行前滚修复
- 需要恢复时，按 `docs/ops/backup-restore.md` 执行恢复或回滚

## 7. 发布工件对齐
- Release notes source of truth: `docs/release/v2.0.4-release-notes.md`
- First-time install source of truth: `docs/ops/install.md`
- Support boundaries: `docs/release/v2-support-boundaries.md`
- Module maturity: `docs/open-source/module-maturity-matrix.md`
