# Community Edition v1.0 GA Stabilization Window

**版本**: `v1.0-ga-window`
**日期**: `2026-03-28`
**作者**: `Codex`

## 当前基线
- 候选标签：`v1.0.0-rc1`
- 候选说明：`https://github.com/IMG-LTD/MMMail/releases/tag/v1.0.0-rc1`
- 候选基线提交：`6cd3bbc`
- 当前规划分支：`dev/community-v1`
- 当前最新 green head：
  - 提交：`1015418`
  - GitHub Actions run：`23675077518`

## 当前阶段目标
- 保持 `dev/community-v1` 最新 head 持续绿灯。
- 使用 `v1.0.0-rc1` 收集真实自托管反馈。
- 在 `v1.0.0` 只接收 release-blocking 修复。
- 不再扩展首发功能或调整首发范围。

## 允许进入 `v1.0.0` 的改动
### `release-blocking`
- `GA` 模块功能回归：
  - `Mail`
  - `Calendar`
  - `Drive`
  - `Auth / Session / MFA`
  - `Organization / RBAC / Admin`
  - `Workspace Shell / Settings`
- 自托管安装、升级、备份恢复、回滚链路出现阻断。
- 安全基线回归：
  - 鉴权绕过
  - 多租户越权
  - 上传 / 下载 / 分享边界失效
  - secrets / session / cookie 默认值失效
- 观测与门禁回归：
  - `validate-local.sh`
  - `validate-all.sh`
  - `validate-ci.sh`
  - `MMMail CI` workflow

### `允许但需最小化`
- 文档勘误，仅限：
  - 安装步骤错误
  - 升级 / 恢复说明错误
  - 发布说明中的事实错误
- Release artifact 元数据修正。

## 明确禁止
- 新增产品功能。
- 扩展 `Docs / Sheets / Billing` 深能力。
- 提升任何 `Preview` 模块成熟度。
- 大范围重构、目录迁移、技术栈替换。
- 为了“更像 Proton”做非阻塞 UI 重做。

## 反馈分流规则
| 反馈类型 | 去向 | 是否阻塞 `v1.0.0` |
|---|---|---|
| `GA` 主路径不可用或数据错误 | `release-blocking` 修复 | 是 |
| 安装、升级、备份恢复文档错误 | `v1.0.0` 最小修复 | 视是否阻断而定 |
| `Docs / Sheets` 能力缺口 | `v1.1` backlog | 否 |
| 国际化缺失或术语不一致 | `v1.1` backlog | 否 |
| Community / Hosted 边界不清 | `v1.1` backlog | 否 |
| `Preview` 模块建议 | `post-v1.1` backlog | 否 |

## 收口标准
`v1.0.0` 发布前必须满足：
1. `dev/community-v1` 最新 head 的 `MMMail CI` 为绿色。
2. 无未关闭的 release-blocking regression。
3. `community-v1-final-signoff.md` 保持有效。
4. `v1.0.0-rc1` 反馈已完成分流。
5. `README`、`support boundaries`、`known issues` 与 release notes 口径一致。

## 执行入口
- 反馈收集：`docs/release/community-v1-feedback-intake.md`
- 当前状态：`docs/release/community-v1-rc-status.md`
- 发布边界：`docs/release/community-v1-support-boundaries.md`
- 最终签收：`docs/release/community-v1-final-signoff.md`
