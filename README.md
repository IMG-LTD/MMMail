# MMMail Community Edition v1.0

隐私优先、模块化、可自托管的协作套件。当前首发交付聚焦 `Mail / Calendar / Drive / Admin / Workspace Shell`，并通过 `Labs` 隔离预览模块。

## 当前发布状态
- `release/v1.0`：`RC1_READY`，承载 `v1.0.0` / `v1.0.x`
- `dev/community-v1`：`v1.1` 集成分支，当前按 `Docs → i18n → Community / Hosted → Sheets` 顺序推进
- 状态说明：`v1.0.0-rc1` prerelease 已发布；`release/v1.0` 已从 `6f86864` 切出，`dev/community-v1` 不再作为 `v1.0.0` 直接发布基线
- 状态文档：`docs/release/community-v1-rc-status.md`
- 当前发布冻结仅适用于 `release/v1.0`；`dev/community-v1` 仅允许推进已批准的 `v1.1` 范围

## 下一步版本节奏
- `v1.0.0-rc1`：已发布，作为首发候选基线。
- `v1.0.0`：由 `release/v1.0` 承载，只处理 release-blocking 缺陷、自托管阻断和最小文档勘误。
- `v1.0.x`：正式发布后继续沿 `release/v1.0` 维护线承接安全修复和 release-blocking backport。
- `v1.1`：由 `dev/community-v1` 承载，按 `Docs → i18n → Community / Hosted → Sheets` 的顺序推进。
- GitHub milestones：`v1.0.0`、`v1.1`、`post-v1.1`
- 不在当前版本节奏中推进 `VPN / Meet / Wallet / Lumo` 等 `Preview` 模块。
- GA 稳定窗口：`docs/release/community-v1-ga-stabilization.md`
- GA 正式发布清单：`docs/release/community-v1-ga-release-checklist.md`
- 反馈收集：`docs/release/community-v1-feedback-intake.md`
- GA 分诊手册：`docs/release/community-v1-ga-triage-playbook.md`
- `v1.1` 规划基线：`docs/release/community-v1-v1.1-plan.md`
- `v1.1` backlog seed：`docs/release/community-v1-v1.1-backlog-seed.md`

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
- 首发范围：`docs/release/community-v1-scope.md`
- 发布门禁：`docs/release/community-v1-gate.md`
- RC 检查清单：`docs/release/community-v1-rc-checklist.md`
- RC1 说明：`docs/release/community-v1-rc1-notes.md`
- `v1.0.0` release notes 草稿：`docs/release/community-v1-v1.0.0-release-notes.md`
- 已知问题：`docs/release/community-v1-known-issues.md`
- 支持边界：`docs/release/community-v1-support-boundaries.md`
- Community / Hosted 边界入口：`/suite` 中的 `Release boundary map`
- 预发布清单：`docs/release/community-v1-pre-release-checklist.md`
- 外部执行清单：`docs/release/external-execution-checklist.md`
- 发布负责人摘要：`docs/release/community-v1-release-manager-brief.md`
- GA 分诊手册：`docs/release/community-v1-ga-triage-playbook.md`
- `v1.1` backlog seed：`docs/release/community-v1-v1.1-backlog-seed.md`
- 外部失败分诊：`docs/release/external-failure-triage.md`
- Gate 回填模板：`docs/release/gate-backfill-template.md`
- 外部回执日志：`docs/release/community-v1-external-receipt-log.md`
- RC 状态：`docs/release/community-v1-rc-status.md`
- 最终签收模板：`docs/release/community-v1-final-signoff.md`
- Freeze exception 模板：`docs/release/freeze-exception-template.md`
- 外部回执后执行清单：`docs/release/post-external-receipt-checklist.md`
- 路线图：`docs/release/community-v1-roadmap.md`
- 模块成熟度：`docs/open-source/module-maturity-matrix.md`

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
