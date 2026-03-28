# Community Edition v1.0 Release Gate

**版本**: `v1.0-rc1-draft`  
**日期**: `2026-03-15`  
**作者**: `Codex`

## 当前 RC1 状态
- 正式状态值：`RC1_READY`
- 依据：GitHub Actions run `23661060407` 已完成官方 CI 回执、dependency-check 报告归档与容器化 RC1 证据链
- 状态判定文档：`docs/release/community-v1-rc-status.md`

## Gate 状态总览

| Gate | 状态 | 证据 | 剩余阻塞 |
|---|---|---|---|
| Gate 0 - 范围冻结 | `PASS` | `docs/release/community-v1-scope.md`、`docs/open-source/module-maturity-matrix.md` | 无 |
| Gate 1 - 安装与部署 | `PASS` | `docs/ops/install.md`、`docker-compose.yml`、`scripts/validate-runtime-env.sh`、`artifacts/release/rc1-container/community-v1-rc1-container-evidence.md`、GitHub Actions run `23661060407` | 无 |
| Gate 2 - 核心功能 | `PASS` | Mail 5A、Calendar 5B、Drive 5C release-blocking 集合已进入默认门禁 | 无 |
| Gate 3 - 数据安全 | `PASS` | Flyway、`docs/ops/upgrade.md`、`docs/ops/backup-restore.md`、`scripts/validate-batch3.sh`、`artifacts/release/rc1-container/community-v1-rc1-container-evidence.md`、GitHub Actions run `23661060407` | 无 |
| Gate 4 - 质量稳定性 | `PASS` | `scripts/validate-local.sh`、`scripts/validate-all.sh`、`scripts/validate-rc1-container.sh`、`artifacts/release/rc1-container/community-v1-rc1-container-evidence.md`、GitHub Actions run `23661060407` | 无 |
| Gate 5 - 安全 | `PASS` | `scripts/validate-security.sh`、`scripts/security-secret-scan.sh`、`scripts/security-backend-dependency-scan.sh`、`docs/security/threat-model.md`、`artifacts/security/dependency-check/dependency-check-report.{html,json}`、GitHub Actions run `23661060407` | 无 |
| Gate 6 - 可运维性 | `PASS` | system-health、Prometheus、error tracking、Runbook、`.github/workflows/ci.yml`、artifact `mmmail-validate-artifacts`、GitHub Actions run `23661060407` | 无 |
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
- secrets 防回归、本地安全回归、threat model、CI 接线与 dependency-check 正式报告归档均已完成

### 外部回填
- 执行日期：`2026-03-28`
- 执行人：`Codex`
- workflow / job：`MMMail CI / validate`
- workflow run：`https://github.com/IMG-LTD/MMMail/actions/runs/23661060407`
- 证据：
  - `artifacts/security/dependency-check/dependency-check-report.html`
  - `artifacts/security/dependency-check/dependency-check-report.json`
  - `artifacts/security/secret-scan.txt`
- 结果：
  - dependency-check：`PASS`
  - secrets scan：`PASS`
  - security regression：`PASS`

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

### 外部回填
- 执行日期：`2026-03-28`
- 执行人：`Codex`
- workflow / job：`MMMail CI / validate`
- workflow run：`https://github.com/IMG-LTD/MMMail/actions/runs/23661060407`
- 证据：
  - artifact：`mmmail-validate-artifacts`
  - logs：`artifacts/ci-logs/`
  - surefire：`backend/mmmail-server/target/surefire-reports/`
  - gate summary：`MMMail CI > validate > Publish gate summary`
- 结果：
  - validate-ci：`PASS`
  - docker-capable runner：`YES`
  - 回执完整性：`PASS`

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

### 外部回填
- 执行日期：`2026-03-28`
- 执行人：`Codex`
- workflow / job：`MMMail CI / validate`
- workflow run：`https://github.com/IMG-LTD/MMMail/actions/runs/23661060407`
- 证据：
  - `artifacts/release/rc1-container/community-v1-rc1-container-evidence.md`
  - `artifacts/release/rc1-container/backups/`
  - `artifacts/release/rc1-container/compose.log`
  - `artifacts/release/rc1-container/db-info.log`
  - `artifacts/release/rc1-container/db-validate.log`
  - `artifacts/release/rc1-container/db-upgrade.log`
  - `artifacts/release/rc1-container/db-backup.log`
  - `artifacts/release/rc1-container/db-restore.log`
  - `artifacts/release/rc1-container/db-rollback.log`
- 结果：
  - fresh install：`PASS`
  - init / seed：`PASS`
  - upgrade：`PASS`
  - backup：`PASS`
  - restore：`PASS`
  - rollback strategy：`PASS`

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
- 无发布门禁阻塞；当前版本已满足 `RC1_READY`
- 后续仅剩发布负责人签收、RC 说明确认与正式发布候选操作
