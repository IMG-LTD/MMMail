# Community Edition v1.0 External Receipt Log

**版本**: `v1.0-rc1-draft`  
**日期**: `2026-03-15`  
**作者**: `Codex`

## 记录规则
- 每次外部执行都新增一行
- 若同一项重跑，保留历史记录，不覆盖旧记录

## 回执登记表
| 日期 | 执行项 | 状态 | workflow / job / runner | 证据路径或链接 | 已回填 Gate | 已解除阻塞 | 执行人 | 备注 |
|---|---|---|---|---|---|---|---|---|
| 待执行 | `MMMail CI / validate` | `PENDING` | `MMMail CI / validate` | `artifacts/security/dependency-check/` | `Gate 5 / Gate 6` | `NO` | 待定 | 需要 Docker-capable runner 与 GitHub Actions |
| 待执行 | `validate-rc1-container.sh` | `PENDING` | `manual runner or CI` | `artifacts/release/rc1-container/` | `Gate 4` | `NO` | 待定 | 需要容器化 install / restore / rollback 回执 |

## 完成后的更新要求
- 将 `状态` 改为 `PASS` 或 `FAIL`
- 填写真实 workflow run 链接或 artifact 路径
- 将 `已回填 Gate` 与 `已解除阻塞` 更新为最终值
