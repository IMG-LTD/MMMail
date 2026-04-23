# Module Maturity Matrix

**版本**: `v2-mainline`
**日期**: `2026-04-23`

| 模块 | 路由 / 入口 | 成熟度 | 默认导航 | 当前说明 |
|---|---|---|---|---|
| Auth / Session / MFA | `/login` `/register` `/security` | `GA` | 否 | 已纳入安全、认证和组织范围回归。 |
| Mail | `/inbox` | `GA` | 是 | 主路径稳定，保留公开邮件分享与附件访问链路。 |
| Calendar | `/calendar` | `GA` | 是 | 维持稳定日程、邀请与可用性工作流。 |
| Drive | `/drive` | `GA` | 是 | 维持共享、下载、公开分享与恢复路径。 |
| Suite Shell | `/suite` | `GA` | 是 | 统一工作区壳层与产品入口。 |
| Business / Organizations | `/business` `/organizations` | `GA` | 是 | 组织、租户、治理与管理员入口。 |
| Settings / Security / System Health | `/settings` `/security` | `GA` | 是 | 包含 system health、安全姿态与设置面板。 |
| Docs | `/docs` | `BETA` | 是 | 保留轻协作工作区，不作为正式发布阻塞。 |
| Sheets | `/sheets` | `BETA` | 是 | 保留轻协作表格工作区，不作为正式发布阻塞。 |
| Pass | `/pass` | `BETA` | 是 | 默认导航可见，但仍按 Beta 口径维护。 |
| Billing | `/suite/billing` | `BETA` | 否 | Community 仅保留计费姿态与 readiness，不承诺真实结算。 |
| Collaboration | `/collaboration` | `PREVIEW` | 否 | 聚合预览面，不进入正式发布承诺。 |
| Command Center | `/command-center` | `PREVIEW` | 否 | 聚合预览面，不进入正式发布承诺。 |
| Notifications | `/notifications` | `PREVIEW` | 否 | 聚合预览面，不进入正式发布承诺。 |
| Labs modules | `/labs/:moduleKey` | `PREVIEW` | 否 | `Authenticator / SimpleLogin / Standard Notes / VPN / Meet / Wallet / Lumo` 均维持预览壳层。 |

## 管理规则
- `GA`：正式承诺能力，必须进入 release gate。
- `BETA`：可见、可用，但不自动成为正式发布阻塞面。
- `PREVIEW`：默认不承诺稳定性，只保留方向验证与试验入口。
