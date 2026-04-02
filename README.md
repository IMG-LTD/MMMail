# MMMail Community Edition

隐私导向、模块化、可自托管的协作套件。当前 Community 主线以 `Web-first` 协作为基线，并通过 `Labs` 隔离预览模块。

## 当前发布状态
- `release/v1.2`：`v1.2.0 RELEASED`
- `release/v1.0`：`v1.0.0 RELEASED`，继续承接 `v1.0.x`
- `dev/community-v1`：`post-v1.2` 主线开发分支，并继续作为后续 `main` 的收敛基线
- 状态说明：`v1.2.0` 已于 `2026-04-02 14:38 +0800` 从 `release/v1.2` 正式切出，对应 tag / commit 为 `v1.2.0` / `38e548a1baa56f70f29841d094fa8f927367b1d9`
- GitHub Release：`https://github.com/IMG-LTD/MMMail/releases/tag/v1.2.0`
- 发布负责人摘要：`docs/release/community-v1-v1.2-release-manager-brief.md`

## 当前版本节奏
- `v1.0.0-rc1`：历史候选基线，反馈已完成分流。
- `v1.0.0`：已正式发布。
- `v1.0.x`：沿 `release/v1.0` 承接安全修复和 release-blocking backport。
- `v1.2.0`：已正式发布，范围冻结为 `PWA baseline / capability honesty / Mail E2EE foundation + recipient discovery + message encryption / architecture discovery / adoption readiness`。
- `v1.2.x`：仅在 `release/v1.2` 上接收 `release-blocking / security / release metadata` 修复。
- `post-v1.2`：由 `dev/community-v1` 承接下一轮已批准范围。
- GitHub milestones：`v1.0.0`、`v1.2`、`post-v1.2`
- 不在当前版本节奏中推进 `VPN / Meet / Wallet / Lumo` 等 `Preview` 模块。

## `v1.2` 能力边界快照
- 已交付：PWA installability、`manifest`、`Service Worker` 注册、离线壳层入口、设置页 PWA readiness、`Mail E2EE foundation`、recipient readiness、`READY` 内部路由正文加密发送与详情页本地解密、OpenAPI / Swagger UI / self-hosted adoption quick pages。
- 受限交付：`Mail E2EE` 当前只覆盖已实现主路径，不包含附件、草稿、外部收件人、Drive 或零知识架构。
- 预研中：`Drive E2EE`、零知识邮件路线、`SMTP / IMAP / Bridge` 分阶段方案。
- 尚未交付：真实 `Web Push` 下发、`iOS / Android / Desktop` 原生客户端、离线写入同步。
- 仅 Hosted：真实支付扣款、商业订阅生命周期、财务级税费 / 发票 / 对账。
- 权威说明：以 `README.md`、`docs/release/community-v1-v1.2-mainline-roadmap.md`、`docs/release/community-v1-v1.2-capability-boundaries.md`、`docs/architecture/mail-zero-knowledge-roadmap.md`、`docs/architecture/mail-protocol-stack-discovery.md`、`/suite` 的 `Release boundary map` 为准。

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
- `v1.2` release manager brief：`docs/release/community-v1-v1.2-release-manager-brief.md`
- `v1.2` release checklist：`docs/release/community-v1-v1.2-release-checklist.md`
- `v1.2` final signoff：`docs/release/community-v1-v1.2-final-signoff.md`
- `v1.2.0` release notes：`docs/release/community-v1-v1.2.0-release-notes.md`
- 支持边界：`docs/release/community-v1-support-boundaries.md`
- `v1.2` 能力边界：`docs/release/community-v1-v1.2-capability-boundaries.md`
- `v1.2` 规划基线：`docs/release/community-v1-v1.2-plan.md`
- `v1.2` 主线路线：`docs/release/community-v1-v1.2-mainline-roadmap.md`
- Community / Hosted 边界入口：`/suite` 中的 `Release boundary map`
- 首发范围：`docs/release/community-v1-scope.md`
- 发布门禁：`docs/release/community-v1-gate.md`
- RC 检查清单：`docs/release/community-v1-rc-checklist.md`
- RC1 说明：`docs/release/community-v1-rc1-notes.md`
- `v1.0.0` release notes：`docs/release/community-v1-v1.0.0-release-notes.md`
- 已知问题：`docs/release/community-v1-known-issues.md`
- 路线图：`docs/release/community-v1-roadmap.md`
- 模块成熟度：`docs/open-source/module-maturity-matrix.md`
- 外部执行清单：`docs/release/external-execution-checklist.md`
- 外部失败分诊：`docs/release/external-failure-triage.md`
- Gate 回填模板：`docs/release/gate-backfill-template.md`
- 外部回执日志：`docs/release/community-v1-external-receipt-log.md`
- 最终签收模板：`docs/release/community-v1-final-signoff.md`
- 外部回执后执行清单：`docs/release/post-external-receipt-checklist.md`

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
