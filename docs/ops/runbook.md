# Community Edition v1.6.1 运维 Runbook

**版本**: `v1.6.1-mainline`
**日期**: `2026-04-09`
**作者**: `Codex`

## 1. 入口清单
- 健康检查：`GET /actuator/health`
- Prometheus 导出：`GET /actuator/prometheus`
- 管理页：`/settings/system-health`
- API 文档：
  - `GET /v3/api-docs`
  - `/swagger-ui.html`
- 浏览器内 API quick page：
  - `/self-hosted/api.html`
- 浏览器内 adoption guide：
  - `/self-hosted/adoption.html`
- Suite boundary 入口：
  - `/suite?section=boundary`
- Labs curated catalog：
  - `/labs`
- 本地门禁：`bash scripts/validate-local.sh`
- CI 门禁：`MMMAIL_VALIDATE_CONTAINER_TESTS=true bash scripts/validate-ci.sh`

## 2. 核心检查
### 服务存活
- Backend：
  - `curl -sf http://127.0.0.1:8080/actuator/health`
- Frontend：
  - 访问 `http://127.0.0.1:3001`
- Frontend PWA：
  - `curl -sf http://127.0.0.1:3001/manifest.webmanifest`
- 系统健康页：
  - 使用管理员账号登录后访问 `/settings/system-health`
- Adoption readiness：
  - 使用管理员账号登录后访问 `/settings`
  - 确认可打开：
    - `adoption guide`
    - `API quick page`
    - `Swagger UI`
    - `OpenAPI JSON`
    - 自托管安装说明 / Runbook 快速页
  - 打开 `/suite?section=overview`
  - 确认主线协作链路可见，且 `Pass` 已作为 `Beta` 入口出现在默认导航
- Suite boundary：
  - 打开 `/suite?section=boundary`
  - 确认 `GA / Beta / Preview / Hosted-only` 说明与 `support boundaries` 文档一致
- Labs curated catalog：
  - 打开 `/labs`
  - 确认默认只显示 `Authenticator / SimpleLogin / Standard Notes`

### Mail E2EE foundation
- 当前只验证已交付主路径：
  - 设置页可以生成并保存 `key profile`
  - `READY` 内部路由发信会对正文加密
  - 草稿保存与恢复走加密链路
  - 附件上传 / 下载走本地加解密链路
  - 邮件详情页可在浏览器内解密正文
  - 外部密码保护加密投递会生成 secure link，并在公开页面完成本地解密与附件下载
- 不要把以下事项当作当前 Runbook 成功条件：
  - 完整 MIME 级外部互通
  - 零知识架构
  - `SMTP inbound / IMAP / Bridge`

### 当前主线的新增聚焦能力
- `Suite sectioned IA`
- `Curated Labs catalog`
- `Runtime a11y gate`（关键入口运行时自动化校验）
- `Drive E2EE foundation`
- `Web Push`
- `SMTP outbound adapter`
- `Calendar internal invitation orchestration`
- `Pass Beta readiness`
- `Mail external password-protected encrypted delivery`

### 本地后端门禁环境
- 默认 `validate-local.sh` 使用后端 `test` profile 回归，不依赖本机 MySQL / Redis / Nacos 实例或真实密钥。
- 如需单独验证 Docs 后端专项回归，可直接执行：
  - `timeout 60s $(bash -lc 'source scripts/lib/java-common.sh; resolve_maven_bin "$PWD"') -f backend/pom.xml -pl mmmail-server -am -Dtest='DocsCollaborationIntegrationTest,DocsSuggestionWorkflowIntegrationTest,DocsOrgAccessIntegrationTest' -Dsurefire.failIfNoSpecifiedTests=false test`
- `DocsOrgAccessIntegrationTest` 会验证 org scope 下 `POST /api/v1/docs/notes`、`POST /api/v1/docs/notes/{id}/comments`、`GET /api/v1/docs/notes/{id}/collaboration` 在产品禁用与强制 2FA 策略下都被统一拒绝。
- 若需要额外验证“本机 live-stack 配置是否完整”，可选执行：
  - 复制 `config/backend.test.env.example` 为 `config/backend.test.env.local`
  - 填入本机实际值
  - 执行 `export MMMAIL_BACKEND_TEST_ENV_FILE=config/backend.test.env.local`
  - 再执行 `bash scripts/validate-backend-test-env.sh`
- 这条可选检查只用于 live-stack 验证，不再阻塞默认本地门禁。

### 日志
- 后端日志为结构化 JSON，包含：
  - `requestId`
  - `event`
  - `module`
  - `userId`
  - `sessionId`
- 排查请求链时，优先按 `requestId` 聚合：
  - `docker compose logs backend | rg '"requestId":"'`

### 指标
- 导出 Prometheus 文本：
  - `curl -s -H "Authorization: Bearer <admin-access-token>" http://127.0.0.1:8080/actuator/prometheus`
- 首发关键指标：
  - `mmmail_api_requests_total`
  - `mmmail_api_requests_failed_total`
  - `mmmail_errors_events_total`
  - `mmmail_jobs_executions_total`

## 3. 常见故障排查
### 系统健康页显示 `DOWN` 或 `DEGRADED`
- 先看 `components` 区块中哪个组件异常。
- `redis=DOWN`：
  - 检查 `SPRING_REDIS_HOST`、`SPRING_REDIS_PORT`、`SPRING_REDIS_PASSWORD`
  - 查看 backend 日志中的 `Redis health check failed`
- `db=DOWN`：
  - 先执行 `./scripts/db-upgrade.sh .env info`
  - 再检查 `SPRING_DATASOURCE_*` 配置和 MySQL 连接性

### `/actuator/prometheus` 返回 `403`
- 该接口仅管理员可访问。
- 先重新登录管理员，再确认请求带 `Authorization: Bearer <token>`。

### `Swagger UI` 或 `OpenAPI JSON` 无法打开
- 先确认后端容器健康：
  - `curl -sf http://127.0.0.1:8080/actuator/health`
- 再检查：
  - `backend/mmmail-server/src/main/resources/application.yml`
  - `backend/mmmail-server/src/main/java/com/mmmail/server/config/WebMvcConfig.java`
  - `backend/mmmail-server/src/main/java/com/mmmail/server/config/SecurityConfig.java`
- 若前端设置页中的文档链接指向错误 origin，检查部署时的 `NUXT_PUBLIC_API_BASE`。

### `/suite` 或 `/labs` 仍显示旧结构
- 先确认浏览器未命中旧缓存。
- 再确认部署产物来自 `v1.6` 分支最新构建。
- 若只有静态页异常，优先排查 CDN / 反向代理缓存，而不是后端接口。

### 外部密码保护加密邮件打不开
- 先确认 secure link 指向的公开页面为：
  - `/share/mail/{token}`
- 再确认后端公开 API 可以访问：
  - `GET /api/v1/public/mail/{token}`
  - `POST /api/v1/public/mail/{token}/access`
- 若公开页面返回 `403` 或 `404`：
  - 检查反向代理是否放行 `/api/v1/public/mail/**`
  - 检查 `SecurityConfig` 与路由映射是否仍保持公开访问
- 若密码校验通过但页面无法解密：
  - 检查发件侧是否为外部收件人生成了 `externalEncryptedPayload`
  - 检查浏览器控制台是否存在 `OpenPGP` 解密异常

### 前端 runtime error 未上报
- 只在已登录会话下上报。
- 检查浏览器控制台是否存在：
  - `Failed to report client runtime error`
- 再检查：
  - `/api/v1/system/errors/client`
  - 后端结构化日志中的 `module=system`

### 后台任务失败
- 系统健康页 `Background jobs` 区块会显示最近任务与失败状态。
- 首发关键后台任务：
  - `MAIL_EASY_SWITCH_IMPORT`
- 若任务失败：
  - 查看后端日志中同 `jobName` 记录
  - 结合 `recentJobs[].detail` 判断失败阶段

## 4. 升级 / 回滚 / 恢复
- 升级前：
  - `./scripts/db-backup.sh .env`
  - `./scripts/db-upgrade.sh .env info`
- 执行升级：
  - `./scripts/db-upgrade.sh .env upgrade`
- 前滚修复：
  - `./scripts/db-upgrade.sh .env repair`
  - `./scripts/db-upgrade.sh .env upgrade`
- 恢复：
  - `./scripts/db-restore.sh .env <backup-dir>`
- 回滚：
  - `./scripts/db-rollback.sh .env <backup-dir>`

## 5. 发布前运维检查
- 本地：
  - `bash scripts/validate-local.sh`
- CI：
  - `MMMAIL_VALIDATE_CONTAINER_TESTS=true bash scripts/validate-ci.sh`
- 手动确认：
  - 管理员可打开 `/settings/system-health`
  - `/actuator/prometheus` 可导出核心指标
  - `/suite?section=boundary` 与 `support boundaries` 文档一致
  - `/labs` 默认 curated catalog 与主线策略一致
  - 最近错误与后台任务列表非空时能正确展示
