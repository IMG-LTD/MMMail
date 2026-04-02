# Module Maturity Matrix

**版本**: `v1.2-release`
**日期**: `2026-04-02`
**作者**: `Codex`

| 模块 | 路由 / 入口 | 成熟度 | 默认导航 | 当前说明 |
|---|---|---|---|---|
| Auth / Session / MFA | `/login` `/register` `/security` | `GA` | 否 | 已纳入 release-blocking 与安全门禁。 |
| Mail | `/inbox` | `GA` | 是 | 主路径稳定；`v1.2` 新增受限 `Mail E2EE` foundation、recipient readiness 与 `READY` 正文加密 / 本地解密。 |
| Calendar | `/calendar` | `GA` | 是 | 维持稳定 GA 工作流。 |
| Drive | `/drive` | `GA` | 是 | 维持稳定 GA 工作流；客户端加密上传仍未交付。 |
| Suite Shell | `/suite` | `GA` | 是 | 统一工作区壳层与入口。 |
| Business / Admin | `/business` `/organizations` | `GA` | 是 | 租户、RBAC、审计和治理入口。 |
| Settings / Security / System Health | `/settings` `/security` | `GA` | 是 | 包含可观测性、安全、PWA readiness、Mail E2EE foundation 与 adoption readiness。 |
| Docs | `/docs` | `BETA` | 是 | 保留为单人 / 轻协作，不作为正式发布阻塞。 |
| Sheets | `/sheets` | `BETA` | 是 | 保留为单人 / 轻协作，不作为正式发布阻塞。 |
| Billing | `Suite > Billing center` | `BETA` | 否 | Community 仅承载报价 / 草稿 / 账单状态，不承诺真实支付闭环。 |
| Pass | `/pass` | `PREVIEW` | 否 | 仅通过 `Labs` 暴露。 |
| Authenticator | `/authenticator` | `PREVIEW` | 否 | `Labs`。 |
| SimpleLogin | `/simplelogin` | `PREVIEW` | 否 | `Labs`。 |
| Standard Notes | `/standard-notes` | `PREVIEW` | 否 | `Labs`。 |
| VPN | `/vpn` | `PREVIEW` | 否 | `Labs`。 |
| Meet | `/meet` | `PREVIEW` | 否 | `Labs`。 |
| Wallet | `/wallet` | `PREVIEW` | 否 | `Labs`。 |
| Lumo | `/lumo` | `PREVIEW` | 否 | `Labs`。 |
| Collaboration / Command Center / Notifications | `/collaboration` `/command-center` `/notifications` | `PREVIEW` | 否 | 只保留方向验证，不进入正式发布承诺。 |

## 管理规则
- `GA`：正式承诺能力，必须进入 release gate。
- `BETA`：可见、可用，但不作为正式发布阻塞。
- `PREVIEW`：默认导航隐藏，只能通过 `Labs` 或显式调试路径进入。
