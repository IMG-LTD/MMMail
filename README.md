# MMMail Community Edition

隐私导向、模块化、可自托管的协作套件。当前 Community 主线坚持 `Web-first` 协作、明确能力边界与 `Labs` 预览隔离，不把未交付能力包装成正式承诺。

## 当前发布状态
- `main`：当前公开发布主线；当前正式发布切点为 `v1.6.0`
- `dev/v1.6`：`v1.6.0` release source branch，已完成合并回 `main`
- `release/v1.2`：保留 `v1.2.x` 历史发布线
- 状态说明：`v1.6.0` 已正式发布，聚焦 `Product Focus & IA Refinement`
- 权威路线：`docs/release/community-v1-v1.6-plan.md`、`docs/release/community-v1-v1.6-mainline-roadmap.md`、`docs/release/community-v1-support-boundaries.md`

## 当前版本节奏
- `v1.0.0-rc1`：历史候选基线，反馈已完成分流。
- `v1.0.0`：已正式发布。
- `v1.0.x`：沿 `release/v1.0` 承接安全修复和 release-blocking backport。
- `v1.2.0`：已正式发布，范围冻结为 `PWA baseline / capability honesty / Mail E2EE foundation + recipient discovery + message encryption / architecture discovery / adoption readiness`。
- `v1.2.x`：仅在 `release/v1.2` 上接收 `release-blocking / security / release metadata` 修复。
- `v1.3.0`：已完成 `Mail E2EE` 深化、`Drive E2EE foundation`、`Web Push`、`SMTP outbound`、`Calendar invite orchestration`、`Pass Beta readiness`、`API docs` 与 `i18n / a11y` 收口。
- `v1.3.1`：历史稳定点，只承接前端依赖安全修复。
- `v1.4.0`：已正式发布，范围冻结为 `Mail 外部密码保护加密投递`。
- `v1.5.0`：已正式发布，聚焦 `Mail external secure attachments + draft reopen + public secure share trust UX`。
- `v1.6.0`：已正式发布，聚焦 `Suite sectioned IA + curated Labs catalog + boundary alignment + runtime a11y gates`。
- GitHub milestones：`v1.0.0`、`v1.2`、`v1.3`、`v1.4`、`v1.5`、`v1.6`
- 不在当前版本节奏中推进 `VPN / Meet / Wallet / Lumo` 等 `Preview` 模块深化。

## `v1.6` 能力边界快照
- 已交付：
  - `/suite` 已从长页混排改为 `Overview / Plans / Billing / Operations / Boundary` 分区视图
  - `/labs` 默认 curated catalog 已收敛为 `Pass / Authenticator / SimpleLogin / Standard Notes`
  - `Release boundary map`、canonical docs、locale 文案与模块成熟度已切到 `v1.6`
  - 运行时无障碍门禁已覆盖关键公开面与核心信息架构入口
  - `PWA` installability、`manifest`、`Service Worker` 注册与浏览器侧 `Web Push`
  - `Mail E2EE` key profile、recipient readiness、`READY` 内部路由正文加密、草稿加密、附件加密、恢复包与详情页本地解密
  - `Mail` 外部密码保护加密投递：浏览器内加密正文与附件、服务端保存密文正文 / 附件与 secure link metadata、草稿恢复 external secure delivery 状态、公开 secure link 页面本地解密正文并下载解密附件
  - `Drive E2EE foundation` 与单文件 `readable-share` E2EE foundation
  - `SMTP outbound adapter`、`Calendar internal invitation orchestration`、`Pass Beta readiness`
- 受限交付：
  - 当前公开发布主线已切到 `v1.6.0`；后续只接收 `v1.6.x` 级别的 `release-blocking / security / metadata` 修复
  - 外部密码保护加密投递当前仍是 `public secure link` 模式，不是完整 MIME 级外部邮箱互通
  - `/labs` 的收敛只影响默认 curated catalog，不改动底层 raw registry / source-of-truth
  - `Pass` 虽已到 `Beta` 口径，但仍只通过 `Labs` 暴露
  - Community `Billing` 只承载报价 / 草稿 / 账单状态，不承诺真实支付闭环
- 预研中：零知识邮件路线、`SMTP / IMAP / Bridge`
- 尚未交付：`iOS / Android / Desktop` 原生客户端、离线写入同步、完整外部 E2EE 协议栈、完整零知识元数据与搜索
- 权威说明：以 `README.md`、`docs/release/community-v1-v1.6-mainline-roadmap.md`、`docs/release/community-v1-support-boundaries.md`、`docs/open-source/module-maturity-matrix.md`、`docs/architecture/mail-zero-knowledge-roadmap.md`、`docs/architecture/mail-protocol-stack-discovery.md`、`/suite` 的 `Release boundary map` 为准。

## 快速启动
1. 准备运行时环境：
   - `cp .env.example .env`
   - 编辑 `.env`，替换所有 `replace-with-*` 占位值
2. 运行环境校验：
   - `./scripts/validate-runtime-env.sh .env`
3. 启动 Community Edition：
   - `docker compose --env-file .env up -d --build`
4. 验证：
   - Frontend：`http://127.0.0.1:3001`
   - Backend：`http://127.0.0.1:8080/actuator/health`

## 本地与 CI 门禁
- 默认本地门禁：`bash scripts/validate-local.sh`
  - 无 Docker daemon 时会显式跳过容器化迁移验证
- 全量门禁：`bash scripts/validate-all.sh`
  - 当前透传 `validate-local.sh`，用于统一本地发布前检查入口
- RC1 本机证据：`bash scripts/validate-rc1-local.sh`
  - 产出 `artifacts/release/rc1-local/community-v1-rc1-local-evidence.md`
- CI 门禁：`bash scripts/validate-ci.sh`
  - 要求 `MMMAIL_VALIDATE_CONTAINER_TESTS=true`
  - 强制执行容器化迁移 / 备份恢复回归
  - 若配置 `MMMAIL_NVD_API_KEY` / GitHub secret `MMMAIL_NVD_API_KEY`，后端依赖扫描会使用该 key 缓存并加速更新

## 文档
- 安装说明：`docs/ops/install.md`
- 升级说明：`docs/ops/upgrade.md`
- 备份恢复：`docs/ops/backup-restore.md`
- 运维 Runbook：`docs/ops/runbook.md`
- 部署拓扑：`docs/architecture/deployment-topology.md`
- 迁移策略：`docs/architecture/database-migration-strategy.md`
- Mail E2EE foundation：`docs/architecture/mail-e2ee-foundation.md`
- Mail E2EE recipient discovery：`docs/architecture/mail-e2ee-recipient-discovery.md`
- Mail E2EE message encryption：`docs/architecture/mail-e2ee-message-encryption.md`
- Mail zero-knowledge roadmap：`docs/architecture/mail-zero-knowledge-roadmap.md`
- Mail protocol stack discovery：`docs/architecture/mail-protocol-stack-discovery.md`
- `v1.6` 规划基线：`docs/release/community-v1-v1.6-plan.md`
- `v1.6` 主线路线：`docs/release/community-v1-v1.6-mainline-roadmap.md`
- `v1.6` release checklist：`docs/release/community-v1-v1.6-release-checklist.md`
- `v1.6` release manager brief：`docs/release/community-v1-v1.6-release-manager-brief.md`
- `v1.6` final signoff：`docs/release/community-v1-v1.6-final-signoff.md`
- `v1.6.0` release notes：`docs/release/community-v1-v1.6.0-release-notes.md`
- 支持边界：`docs/release/community-v1-support-boundaries.md`
- Community / Hosted 边界入口：`/suite` 中的 `Release boundary map`
- 首发范围：`docs/release/community-v1-scope.md`
- 发布门禁：`docs/release/community-v1-gate.md`
- RC 检查清单：`docs/release/community-v1-rc-checklist.md`
- RC1 说明：`docs/release/community-v1-rc1-notes.md`
- 已知问题：`docs/release/community-v1-known-issues.md`
- 路线图：`docs/release/community-v1-roadmap.md`
- 模块成熟度：`docs/open-source/module-maturity-matrix.md`
- 外部执行清单：`docs/release/external-execution-checklist.md`
- 外部失败分诊：`docs/release/external-failure-triage.md`

## 可观测性入口
- 健康检查：`http://127.0.0.1:8080/actuator/health`
- Prometheus 导出：`http://127.0.0.1:8080/actuator/prometheus`
- 管理页：登录管理员后访问 `http://127.0.0.1:3001/settings/system-health`
- 关键指标：
  - `mmmail_api_requests_total`
  - `mmmail_api_requests_failed_total`
  - `mmmail_errors_events_total`
  - `mmmail_jobs_executions_total`

## 安全
- 安全策略：`SECURITY.md`
- 威胁模型：`docs/security/threat-model.md`
- 安全门禁：
  - `bash scripts/validate-security.sh`
  - `MMMAIL_RUN_BACKEND_DEPENDENCY_SCAN=true bash scripts/validate-security.sh`
  - 依赖扫描报告输出到 `artifacts/security/dependency-check/`
- 生产部署建议：
  - 启用 HTTPS，并显式设置 `MMMAIL_AUTH_COOKIE_SECURE=true`
  - 轮转 `.env` 中所有 secrets
  - 仅向受信网段暴露管理接口与 `actuator`

## 开源协作
- 许可证：`LICENSE`
- 贡献指南：`CONTRIBUTING.md`
- Release notes 模板：`docs/release/release-notes-template.md`
- 外部 CI 交接：`docs/release/external-ci-handoff.md`
- Issue / PR 模板：`.github/ISSUE_TEMPLATE/`、`.github/pull_request_template.md`
