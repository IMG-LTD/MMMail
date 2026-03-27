# Community Edition v1.0 Release Gate

**版本**: `v1.0-rc1-draft`  
**日期**: `2026-03-15`  
**作者**: `Codex`

## 当前 RC1 状态
- 正式状态值：`RC1_READY_PENDING_EXTERNAL`
- 依据：本机可完成项已全部收口，剩余均为外部 Docker-capable / GitHub Actions 官方回执
- 状态判定文档：`docs/release/community-v1-rc-status.md`

## Gate 状态总览

| Gate | 状态 | 证据 | 剩余阻塞 |
|---|---|---|---|
| Gate 0 - 范围冻结 | `PASS` | `docs/release/community-v1-scope.md`、`docs/open-source/module-maturity-matrix.md` | 无 |
| Gate 1 - 安装与部署 | `IN_PROGRESS` | `docs/ops/install.md`、`docker-compose.yml`、`scripts/validate-runtime-env.sh`、`scripts/validate-rc1-local.sh`、`artifacts/release/rc1-local/community-v1-rc1-local-evidence.md` | 缺少 Docker-capable fresh install / init 官方回执 |
| Gate 2 - 核心功能 | `PASS` | Mail 5A、Calendar 5B、Drive 5C release-blocking 集合已进入默认门禁 | 无 |
| Gate 3 - 数据安全 | `IN_PROGRESS` | Flyway、`docs/ops/upgrade.md`、`docs/ops/backup-restore.md`、`scripts/validate-batch3.sh`、`artifacts/release/rc1-local/community-v1-rc1-local-evidence.md` | Docker-capable CI runner 的容器化迁移 / 恢复正式回执 |
| Gate 4 - 质量稳定性 | `IN_PROGRESS` | `scripts/validate-local.sh`、`scripts/validate-all.sh`、`scripts/validate-rc1-local.sh`、`scripts/validate-rc1-container.sh`、`artifacts/release/rc1-local/community-v1-rc1-local-evidence.md` | 仅剩 Docker-capable install / upgrade / restore / rollback 正式回执 |
| Gate 5 - 安全 | `PASS_CANDIDATE` | `scripts/validate-security.sh`、`scripts/security-secret-scan.sh`、`scripts/security-backend-dependency-scan.sh`、`docs/security/threat-model.md`、`SecurityBaselineIntegrationTest` | 等待 CI 执行后端 dependency scan 并归档正式报告 |
| Gate 6 - 可运维性 | `BLOCKED_EXTERNAL` | system-health、Prometheus、error tracking、Runbook、`.github/workflows/ci.yml` artifact/summary | 当前环境无 Git remote / `gh`，无法直接产出远端 Actions 正式回执 |
| Gate 7 - 开源治理 | `PASS` | `LICENSE`、`CONTRIBUTING.md`、`README.md`、`docs/release/community-v1-roadmap.md`、issue / PR 模板 | 无 |

## Gate 5 - 安全基线

### 已完成
- 仓库凭据清理与占位符模板
- 登录失败限流
- 客户端错误上报限流
- API 响应安全头：
  - `X-Frame-Options: DENY`
  - `X-Content-Type-Options: nosniff`
  - `Referrer-Policy: no-referrer`
  - API 级 `Content-Security-Policy`
  - API 级 `Permissions-Policy`
- 认证 / 上传 / 下载 / 分享 / 管理员接口回归整理为安全门禁
- secrets 防回归扫描脚本
- 后端 OWASP Dependency-Check 脚本与 CI 接入
- `docs/security/threat-model.md`

### 本地验证
- `bash scripts/validate-security.sh`
- `timeout 60s ... -Dtest=SecurityBaselineIntegrationTest test`
- `bash scripts/validate-local.sh`
- `bash scripts/validate-all.sh`

### 当前状态
- secrets 防回归、本地安全回归、threat model、CI 接线均已落地
- 依赖漏洞扫描的“正式报告归档”仍等待远端 CI workflow 产出 artifact

### CI 目标回执
- workflow: `.github/workflows/ci.yml`
- step summary: `MMMail CI > validate > Publish gate summary`
- artifact: `mmmail-validate-artifacts`
- dependency report: `artifacts/security/dependency-check/dependency-check-report.{html,json}`
- 外部交接：`docs/release/external-ci-handoff.md`

## Gate 6 - 可运维性与 CI 回执

### 已完成
- `scripts/validate-ci.sh` 强制：
  - `MMMAIL_VALIDATE_CONTAINER_TESTS=true`
  - `MMMAIL_RUN_BACKEND_DEPENDENCY_SCAN=true`
- `.github/workflows/ci.yml` 已上传：
  - `artifacts/security/`
  - `artifacts/ci-logs/`
  - `backend/mmmail-server/target/surefire-reports/`
- CI 已缓存：
  - Maven 依赖
  - `.tools/dependency-check-data`
- `MMMAIL_NVD_API_KEY` 若在 CI secret 中存在，会自动用于加速 NVD 更新

### 当前外部阻塞
- 当前仓库未配置 Git remote
- 当前环境无 `gh` CLI
- 因此无法从本机直接触发 GitHub Actions 形成“官方 CI 回执”

### 结论
- CI 链路定义已落地
- 本地与统一门禁可执行
- 官方回执仍依赖远端仓库连接后的首次 workflow 运行；在此之前 Gate 6 保持 `BLOCKED_EXTERNAL`

## Gate 4 - RC1 安装 / 升级证据

### 本机已完成
- `scripts/validate-rc1-local.sh`
  - `validate-runtime-env`
  - `docker compose config`
  - `validate-local`
  - `validate-all`
- 本机证据归档：
  - `artifacts/release/rc1-local/community-v1-rc1-local-evidence.md`
  - `artifacts/release/rc1-local/runtime-env.log`
  - `artifacts/release/rc1-local/compose-config.log`
  - `artifacts/release/rc1-local/validate-local.log`
  - `artifacts/release/rc1-local/validate-all.log`

### 本机未完成项（仅因外部环境）
- Docker daemon 不可用，无法在当前宿主执行：
  - `scripts/validate-rc1-container.sh`
  - fresh install / init / seed live 回执
  - upgrade / backup / restore / rollback 容器化回执

### 外部待执行
- `scripts/validate-rc1-container.sh`
  - fresh install
  - init / seed 校验
  - upgrade
  - backup
  - restore
  - rollback strategy
- 外部交接：`docs/release/external-ci-handoff.md`
- 期望归档：
  - `artifacts/release/rc1-container/community-v1-rc1-container-evidence.md`
  - `artifacts/release/rc1-container/backups/`

## Gate 7 - 开源治理

### 已完成
- `LICENSE`
- `CONTRIBUTING.md`
- `SECURITY.md`
- `README.md`
- `docs/release/community-v1-roadmap.md`
- `docs/release/release-notes-template.md`
- `docs/release/community-v1-rc-checklist.md`
- `.github/ISSUE_TEMPLATE/*`
- `.github/pull_request_template.md`

### 一致性来源
- 范围：`docs/release/community-v1-scope.md`
- 模块分级：`docs/open-source/module-maturity-matrix.md`
- 发布门禁：`docs/release/community-v1-gate.md`
- 安全入口：`SECURITY.md`、`docs/security/threat-model.md`

### 本地验证
- `bash scripts/validate-local.sh`
- `bash scripts/validate-all.sh`

## 当前最小剩余任务
1. 在 Docker-capable 远端 runner 上运行一次 `MMMail CI` workflow，收集 Gate 3 / 5 / 6 正式回执与 dependency-check artifact
2. 在同一 runner 执行 `scripts/validate-rc1-container.sh`，记录 Gate 1 / Gate 4 install / restore 回执
3. 将 CI artifact 与 gate 状态回填到本文件和 `docs/release/community-v1-rc-checklist.md`
4. 更新 `docs/release/community-v1-external-receipt-log.md` 与 `docs/release/community-v1-final-signoff.md`
