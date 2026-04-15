# Community Edition v1.6.1 Feedback Intake

**版本**: `v1.6.1`
**日期**: `2026-04-15`

## 目标
- 以 `v1.6.1` 当前公开基线为默认反馈基线。
- 将反馈严格分流到：
  - `release-blocking`
  - `docs / self-hosting correction`
  - `P0 / P1 / P2 / P3 backlog`
  - `Hosted backlog`

## 当前基线
- 公开基线：`main` / `Community Edition v1.6.1`
- 当前 issue 分流只服务于当前公开基线，不再公开暴露历史收口分支说明。
- 权威 release 说明：
  - `docs/release/community-v1-v1.6.1-release-notes.md`
  - `docs/release/community-v1-support-boundaries.md`

## GitHub milestones
- `release-blocking`：当前公开基线断裂或安全 / 数据风险
- `P0`：主线协作闭环深度
- `P1`：`Drive E2EE / Pass Beta` 深化
- `P2`：identity readiness、developer docs、team enablement
- `P3`：Preview pluginization / externalization
- `Hosted backlog`：真实支付、商业 SLA、企业自动化等不进入 Community 的能力

## 提交入口
### `release-blocking` 回归
- 使用模板：`.github/ISSUE_TEMPLATE/release-blocking-regression.md`
- 适用场景：
  - `GA` 主路径失败
  - 安装 / 升级 / 恢复阻断
  - 已公开披露或已修复后的安全 / 越权回归
  - 最新 `main`、活跃 `dev/v*` 或 `release/v*` workflow 失绿且影响公开基线

### 一般缺陷
- 使用模板：`.github/ISSUE_TEMPLATE/bug-report.md`
- 适用场景：
  - `GA` 非阻塞缺陷
  - `Beta` 缺陷
  - `Preview / Labs` 缺陷（产品宽度问题不会升级为 `release-blocking`，但安全问题仍按 `SECURITY.md` 处理）
  - 非自托管类公开文档错误
- 若涉及 live security vulnerability 或未公开披露的越权问题，不要公开提交 issue，改按 `SECURITY.md` 处理。

### 自托管反馈
- 使用模板：`.github/ISSUE_TEMPLATE/self-hosting-feedback.md`
- 适用场景：
  - 非阻塞的 Docker Compose 安装 / 升级 / 恢复反馈
  - 运维与部署拓扑反馈
  - install / upgrade / runbook 等自托管文档与脚本之间的不一致
- 若问题阻塞当前公开基线，改走 `.github/ISSUE_TEMPLATE/release-blocking-regression.md`

### 需求与 backlog
- 使用模板：`.github/ISSUE_TEMPLATE/feature-request.md`
- 适用场景：
  - `P0 / Mainline collaboration depth`
  - `P1 / Drive E2EE / Pass Beta depth`
  - `P2 / Identity readiness / developer docs / team enablement`
  - `P3 / Preview pluginization / externalization`
  - `Hosted backlog / commercial`
- 文档勘误不要走 feature backlog。
- 分流与优先级说明：`docs/release/community-v1-roadmap.md`

## 分流矩阵
| 反馈 | 归类 | 动作 |
|---|---|---|
| `Mail / Calendar / Drive / Auth / RBAC` 主路径断裂 | `release-blocking` | 立即进入当前公开基线修复 |
| Docker Compose / install / upgrade / backup / restore 无法按文档完成 | `release-blocking` 或 `docs / self-hosting correction` | 先判定是否阻断 |
| `Mail → Calendar → Drive → Pass` 交接链路有明显断点 | `P0` | 不混入 Preview 宽度需求 |
| `Drive E2EE` 消费 / 恢复 / readable-share 体验不足 | `P1` | 进入主线深化，不扩新模块 |
| `Pass` 交接体验、secret handoff 或 Beta 边界表达不足 | `P1` | 进入主线深化，不夸大未交付能力 |
| `SSO / SCIM / LDAP readiness`、developer docs、team enablement 不清楚 | `P2` | 作为 readiness / guidance backlog |
| `Billing` 真实支付、税费、发票下载、商业结算 | `Hosted backlog` | 不进入 Community `v1.6.1` |
| `VPN / Meet / Wallet / Lumo` 建议 | `P3` 或更后 | 默认不阻塞当前主线 |

## 必要信息
提交 issue 时至少提供：
- 版本：`v1.6.1`、具体 commit 或当前 `main`
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
- 当前公开基线只接收 release-blocking 修复与最小必要勘误。
- 非阻塞问题不混入当前发布窗口。
- 无法证明影响 `GA` 主路径的问题，不升级为 release-blocking。
- 自托管反馈优先使用专用模板，避免与功能缺陷、功能需求混在一起。
- backlog 以 `P0 → P1 → P2 → P3` 顺序进入，不因 Preview 宽度打断主线收口。
