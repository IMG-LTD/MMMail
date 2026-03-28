# Community Edition v1.0 External Execution Checklist

**版本**: `v1.0-rc1-draft`  
**日期**: `2026-03-15`  
**作者**: `Codex`

## 当前状态
- 当前仓库正式状态：`RC1_READY`
- 解释：
  - 外部执行与官方 CI 回执已完成
  - 本清单保留为执行记录与后续复用模板

## 执行前检查
- [ ] 仓库已连接 Git remote，且可触发 GitHub Actions
- [ ] runner 具备 Docker daemon 能力
- [ ] 已配置 GitHub Actions secret：`MMMAIL_NVD_API_KEY`（必需；未配置时 `validate` 会快速失败）
- [ ] 外部执行人已阅读：
  - `docs/release/external-ci-handoff.md`
  - `docs/release/gate-backfill-template.md`
  - `docs/release/community-v1-rc-status.md`

## 执行清单
| 序号 | 执行项 | Workflow / Job | 前置条件 | Secrets / Env | 预期产物 | 通过标准 | 失败排查顺序 | 回填位置 |
|---|---|---|---|---|---|---|---|---|
| 1 | 运行官方 CI 门禁 | `MMMail CI / validate` | Git remote、GitHub Actions 可用、Docker-capable runner | `MMMAIL_VALIDATE_CONTAINER_TESTS=true`、`MMMAIL_RUN_BACKEND_DEPENDENCY_SCAN=true`、`MMMAIL_NVD_API_KEY`（必需） | `artifacts/security/dependency-check/dependency-check-report.html`、`artifacts/security/dependency-check/dependency-check-report.json`、`artifacts/ci-logs/`、`backend/mmmail-server/target/surefire-reports/` | `frontend` / `backend` / `validate` 三个 job 全绿 | 先看 GitHub Step Summary，再看 `artifacts/ci-logs/`，最后看 `backend/mmmail-server/target/surefire-reports/` | `docs/release/community-v1-gate.md` 的 `## Gate 5 - 安全基线`、`## Gate 6 - 可运维性与 CI 回执` |
| 2 | 运行容器化 RC1 证据链 | 手工执行 `bash scripts/validate-rc1-container.sh` 或并入远端 CI | Docker daemon、Compose、MySQL/Redis/Nacos/前后端容器可起 | 同 `.env` 模板；若接入 CI 复用 `MMMAIL_NVD_API_KEY` | `artifacts/release/rc1-container/community-v1-rc1-container-evidence.md`、`artifacts/release/rc1-container/backups/`、`artifacts/release/rc1-container/compose.log` | fresh install、init/seed、upgrade、backup、restore、rollback 全为 `PASS` | 先看 `community-v1-rc1-container-evidence.md`，再看 `compose.log`，再看 `db-*.log` | `docs/release/community-v1-gate.md` 的 `## Gate 4 - RC1 安装 / 升级证据` |
| 3 | 归档并登记外部回执 | 手工回填 | 上述两项都完成 | workflow run 链接、artifact 路径、执行日期、执行人 | 更新后的 gate / checklist / receipt log | Gate 4/5/6 的证据段完成回填，receipt log 记录完整 | 对照 `docs/release/gate-backfill-template.md` 查漏补缺 | `docs/release/community-v1-rc-checklist.md`、`docs/release/community-v1-pre-release-checklist.md`、`docs/release/community-v1-external-receipt-log.md` |
| 4 | 形成 RC1 可签收状态 | 手工回填 + 签收 | Gate 4/5/6 已达 PASS 条件 | 外部回执链接、artifact 路径 | 更新后的 `docs/release/community-v1-rc-status.md`、`docs/release/community-v1-final-signoff.md` | 状态切换为 `RC1_READY`，最终签收模板可勾选完成 | 若任一 Gate 未 PASS，回到对应 gate 模板补证 | `docs/release/community-v1-rc-status.md` 的 `## 状态迁移条件`、`docs/release/community-v1-final-signoff.md` |

## 执行顺序
1. 先执行 `MMMail CI / validate`
2. 再执行 `bash scripts/validate-rc1-container.sh`
3. 回填 Gate 4 / Gate 5 / Gate 6
4. 更新 RC Checklist / Pre-release Checklist / Receipt Log
5. 更新 `community-v1-rc-status.md`
6. 勾选 `community-v1-final-signoff.md`

## 执行后必须更新的文档
- `docs/release/community-v1-gate.md`
  - `## Gate 状态总览`
  - `## Gate 5 - 安全基线`
  - `## Gate 6 - 可运维性与 CI 回执`
  - `## Gate 4 - RC1 安装 / 升级证据`
- `docs/release/community-v1-rc-checklist.md`
- `docs/release/community-v1-pre-release-checklist.md`
- `docs/release/community-v1-external-receipt-log.md`
- `docs/release/community-v1-rc-status.md`
- `docs/release/community-v1-final-signoff.md`

## 解除阻塞判定
- Gate 4 解除阻塞：
  - `community-v1-rc1-container-evidence.md` 已归档
  - install / upgrade / backup / restore / rollback 证据已写入 gate
- Gate 5 解除阻塞：
  - dependency-check `html/json` 报告已归档
  - Gate 5 已从 `PASS_CANDIDATE` 更新为 `PASS`
- Gate 6 解除阻塞：
  - GitHub Actions 官方 run 链接、artifact 路径、Step Summary 已回填
  - Gate 6 已从 `BLOCKED_EXTERNAL` 更新为 `PASS`
