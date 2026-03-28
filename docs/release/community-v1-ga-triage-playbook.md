# Community Edition v1.0 GA Triage Playbook

**版本**: `v1.0-ga-triage`
**日期**: `2026-03-28`
**作者**: `Codex`

## 目标
- 把 `v1.0.0` 稳定窗口内的新反馈快速分流到正确轨道。
- 保证只有真正阻塞 `GA` 的问题进入 `release-blocking`。
- 保证 `v1.1` backlog 只进入既定四条主线。

## 分诊入口
- `release-blocking` 回归：`.github/ISSUE_TEMPLATE/release-blocking-regression.md`
- 一般缺陷：`.github/ISSUE_TEMPLATE/bug-report.md`
- 自托管反馈：`.github/ISSUE_TEMPLATE/self-hosting-feedback.md`
- 需求与 backlog：`.github/ISSUE_TEMPLATE/feature-request.md`

## 先决检查
分诊前必须先确认：
1. 反馈是否基于 `v1.0.0-rc1` 或 `dev/community-v1` 最新 green head。
2. 是否附带复现步骤、日志、截图、健康页或 workflow run 证据。
3. 是否已经存在重复 issue。

## 分诊决策树
### 进入 `release-blocking`
满足任一条件即可进入 `v1.0.0` milestone：
- `Mail / Calendar / Drive / Auth / RBAC / Admin / Workspace Shell` 主路径中断。
- 安装、升级、备份恢复、回滚链路无法按文档完成。
- 多租户越权、鉴权绕过、上传/下载/分享权限边界失效。
- 默认门禁或 `MMMail CI` 失败，且影响候选发布。

### 进入 `v1.0.0` 文档勘误
满足以下条件：
- 问题属实，但只需要最小文档修正即可消除歧义。
- 不涉及功能实现缺陷。
- 不影响当前 `GA` 主路径可用性。

### 进入 `v1.1`
仅允许进入以下四条主线：
- `Docs`
- `Sheets`
- `i18n`
- `Community / Hosted`

### 进入 `post-v1.1`
- `Preview` 模块建议。
- 新产品面、重型协同引擎、真实支付链路、媒体引擎或网络隧道能力。

## 标签与 milestone 规则
| 类型 | 必带 labels | milestone |
|---|---|---|
| `release-blocking` | `bug`, `release-blocking`, `ga-stabilization` | `v1.0.0` |
| 一般缺陷 | `bug`, `needs-triage` | 视分诊结果决定 |
| 自托管反馈 | `needs-triage`, `self-hosting-feedback` | `v1.0.0` 或 `v1.1` |
| `v1.1 Docs` | `enhancement`, `needs-triage`, `v1.1-docs` | `v1.1` |
| `v1.1 Sheets` | `enhancement`, `needs-triage`, `v1.1-sheets` | `v1.1` |
| `v1.1 i18n` | `enhancement`, `needs-triage`, `v1.1-i18n` | `v1.1` |
| `Community / Hosted` | `enhancement`, `needs-triage`, `community-hosted-boundary` | `v1.1` |

## 处理时限
- `release-blocking`：当天确认、当天给出处理路径。
- 自托管阻断反馈：当天确认是否阻断 `GA`。
- `v1.1` backlog：48 小时内完成标签与 milestone 分流。

## 关闭条件
- `release-blocking`：修复已合并，默认门禁恢复绿色，并在 issue 中回填 commit / workflow run。
- 文档勘误：文档已合并并在 issue 中附具体文件路径。
- `v1.1` backlog：已打标签、挂 milestone、写清所属 stream。
