# Community Edition v1.0 External CI Handoff

**版本**: `v1.0-rc1-draft`  
**日期**: `2026-03-15`  
**作者**: `Codex`

## 目标
- 为远端仓库管理员 / 发布经理提供一套无需额外解释即可执行的 RC1 外部回执流程。
- 收集 Gate 3 / Gate 4 / Gate 5 / Gate 6 所需的官方 CI 回执与归档 artifact。

## 推荐阅读顺序
1. `docs/release/external-execution-checklist.md`
2. `docs/release/gate-backfill-template.md`
3. `docs/release/community-v1-rc-status.md`
4. `docs/release/community-v1-final-signoff.md`

## 前置条件
- 仓库已连接到可执行 GitHub Actions 的远端 Git remote。
- 远端 runner 必须是 **Docker-capable**。
- 允许执行：
  - `bash scripts/validate-ci.sh`
  - `bash scripts/validate-rc1-container.sh`
- 推荐配置 GitHub Actions secret：
  - `MMMAIL_NVD_API_KEY`：可选，但强烈建议，用于加速 `dependency-check` 更新。

## 触发路径
### GitHub Actions
- Workflow：`MMMail CI`
- 关键 job：`validate`
- 触发方式：
  - push 到 `main` / `master`
  - PR
  - 或在 GitHub Actions 页面手工 rerun `validate`

### 手工 runner
- 在 Docker-capable 机器执行：
  - `MMMAIL_VALIDATE_CONTAINER_TESTS=true MMMAIL_RUN_BACKEND_DEPENDENCY_SCAN=true bash scripts/validate-ci.sh`

## 远端执行内容
- `scripts/validate-local.sh`
- `scripts/validate-batch3.sh`（容器化迁移 / 备份恢复）
- `scripts/validate-security.sh`（含 dependency-check）
- `scripts/validate-rc1-container.sh`（fresh install / init / upgrade / backup / restore / rollback）

## 期望产物
- GitHub Actions Step Summary：
  - `MMMail CI > validate > Publish gate summary`
- 上传 artifact：
  - `artifacts/security/dependency-check/dependency-check-report.html`
  - `artifacts/security/dependency-check/dependency-check-report.json`
  - `artifacts/release/rc1-container/community-v1-rc1-container-evidence.md`
  - `artifacts/release/rc1-container/backups/`
  - `artifacts/ci-logs/`
  - `backend/mmmail-server/target/surefire-reports/`
- 本机已完成参考：
  - `artifacts/release/rc1-local/community-v1-rc1-local-evidence.md`

## 通过标准
- `frontend` job 绿色
- `backend` job 绿色
- `validate` job 绿色
- `community-v1-rc1-container-evidence.md` 中所有步骤均为 `PASS`
- `dependency-check-report.html` 与 `dependency-check-report.json` 均已归档

## Gate 回填位置
- Gate 4：`docs/release/community-v1-gate.md`
  - 更新 fresh install / upgrade / backup / restore / rollback 的状态与 artifact 路径
- Gate 5：`docs/release/community-v1-gate.md`
  - 将 dependency-check 正式报告路径写入 Gate 5 证据段
- Gate 6：`docs/release/community-v1-gate.md`
  - 将 `BLOCKED_EXTERNAL` 改为 `PASS` 或 `PASS_CANDIDATE`
- RC Checklist：`docs/release/community-v1-rc-checklist.md`
  - 勾选 `backend dependency scan 报告已归档`
  - 勾选 `validate-ci workflow 已产出官方回执`
- Receipt Log：`docs/release/community-v1-external-receipt-log.md`
  - 记录执行状态、证据链接、是否已解除阻塞
- RC 状态：`docs/release/community-v1-rc-status.md`
  - 将 `RC1_READY_PENDING_EXTERNAL` 更新为 `RC1_READY`

## 失败排查
- `validate` job 失败：
  - 查看 `artifacts/ci-logs/`
  - 查看 `backend/mmmail-server/target/surefire-reports/`
- `dependency-check` 超时或失败：
  - 检查 `MMMAIL_NVD_API_KEY`
  - 检查 `.tools/dependency-check-data` cache 是否命中
- `validate-rc1-container.sh` 失败：
  - 查看 `artifacts/release/rc1-container/compose.log`
  - 查看 `artifacts/release/rc1-container/db-*.log`
  - 查看 `artifacts/release/rc1-container/community-v1-rc1-container-evidence.md`
