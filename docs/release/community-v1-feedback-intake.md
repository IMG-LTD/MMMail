# Community Edition v1.0 Feedback Intake

**版本**: `v1.0-feedback-intake`
**日期**: `2026-03-31`
**作者**: `Codex`

## 目标
- 以 `v1.0.0` 正式版为当前反馈入口。
- 将反馈严格分流到：
  - `release-blocking`
  - `v1.0.x` 文档 / 安装勘误
  - `v1.1`
  - `post-v1.1`

## 当前发布基线
- 当前 tag：`v1.0.0`
- 当前 Release：`https://github.com/IMG-LTD/MMMail/releases/tag/v1.0.0`
- 当前发布提交：`829c60b`
- 历史候选 tag：`v1.0.0-rc1`
- 历史候选提交：`6cd3bbc`
- 维护分支：`release/v1.0`
- `v1.1` 集成分支：`dev/community-v1`

## GitHub milestones
- `v1.0.0`：`release-blocking` 与自托管阻断项
- `v1.1`：`Docs / Sheets / i18n / Community / Hosted`
- `post-v1.1`：`Preview` 与延后能力

## 提交入口
### `release-blocking` 回归
- 使用模板：`.github/ISSUE_TEMPLATE/release-blocking-regression.md`
- 适用场景：
  - `GA` 主路径失败
  - 安装 / 升级 / 恢复阻断
  - 安全或越权回归
  - `release/v1.0` 最新维护 head 变红且影响 `v1.0.x`

### 一般缺陷
- 使用模板：`.github/ISSUE_TEMPLATE/bug-report.md`
- 适用场景：
  - `GA` 非阻塞缺陷
  - `Beta` 缺陷
  - 文档错误

### 自托管反馈
- 使用模板：`.github/ISSUE_TEMPLATE/self-hosting-feedback.md`
- 适用场景：
  - Docker Compose 安装
  - 升级 / 恢复
  - 运维与部署拓扑反馈
  - 文档与脚本之间的不一致

### 需求与 backlog
- 使用模板：`.github/ISSUE_TEMPLATE/feature-request.md`
- 适用场景：
  - `Docs / Sheets`
  - 国际化
  - Community / Hosted 边界
  - 其他 post-v1.0 能力
- 分诊手册：`docs/release/community-v1-ga-triage-playbook.md`
- `v1.1` backlog seed：`docs/release/community-v1-v1.1-backlog-seed.md`

## 分流矩阵
| 反馈 | 归类 | 动作 |
|---|---|---|
| `Mail / Calendar / Drive / Auth / RBAC` 主路径断裂 | `release-blocking` | 进入 `v1.0.x` |
| Docker Compose / install / upgrade / backup / restore 无法按文档完成 | `release-blocking` 或文档勘误 | 先判定是否阻断 |
| `Docs / Sheets` 编辑体验不足 | `v1.1` | 不阻塞 |
| 简繁英缺失、术语不统一 | `v1.1` | 不阻塞 |
| `Billing` 真实支付需求 | `Hosted backlog` | 不进入 Community `v1.0.x` |
| `VPN / Meet / Wallet / Lumo` 建议 | `post-v1.1` | 不阻塞 |

## 必要信息
提交 issue 时至少提供：
- 版本：`v1.0.0` / `v1.0.0-rc1` 或具体 commit
- 部署方式：Docker Compose / 本地开发 / CI
- 环境信息：浏览器、OS、数据库版本
- 复现步骤
- 实际结果与预期结果
- 日志 / 截图 / 健康页证据
- 若与门禁相关，附：
  - `bash scripts/validate-local.sh`
  - `bash scripts/validate-all.sh`
  - 对应 workflow run 链接

## 当前处理原则
- `v1.0.x` 只接收 release-blocking 修复与最小文档勘误。
- 非阻塞问题不混入当前发布窗口。
- 无法证明影响 `GA` 主路径的问题，不升级为 release-blocking。
- 自托管反馈优先使用专用模板，避免与功能缺陷、功能需求混在一起。
- `v1.1` backlog 仅允许进入 `Docs / Sheets / i18n / Community / Hosted` 四条主线。
