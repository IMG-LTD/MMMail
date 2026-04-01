# Module Maturity Matrix

**版本**: `v1.0-draft`  
**日期**: `2026-03-13`  
**作者**: `Codex`

| 模块 | 路由 / 入口 | 成熟度 | 默认导航 | 当前说明 |
|---|---|---|---|---|
| Auth / Session / MFA | `/login` `/register` `/security` | `GA` | 否 | 已纳入 release-blocking 与安全门禁。 |
| Mail | `/inbox` | `GA` | 是 | 5A 已收口，作为 Gate 2 阻塞集合。 |
| Calendar | `/calendar` | `GA` | 是 | 5B 已收口，作为 Gate 2 阻塞集合。 |
| Drive | `/drive` | `GA` | 是 | 5C 已收口，作为 Gate 2 阻塞集合。 |
| Suite Shell | `/suite` | `GA` | 是 | 统一工作区壳层与入口。 |
| Business / Admin | `/business` `/organizations` | `GA` | 是 | 租户、RBAC、审计和治理入口。 |
| Settings / Security / System Health | `/settings` `/security` | `GA` | 是 | 包含可观测性、安全与个人设置。 |
| Docs | `/docs` | `BETA` | 是 | 保留为单人 / 轻协作，不阻塞首发。 |
| Sheets | `/sheets` | `BETA` | 是 | 保留为单人 / 轻协作，不阻塞首发。 |
| Billing | `Suite > Billing center` | `BETA` | 否 | Community 仅承载报价 / 草稿 / 账单状态，作为 `Suite` 子入口暴露，不承诺真实支付闭环。 |
| Pass | `/pass` | `PREVIEW` | 否 | 仅通过 `Labs` 暴露。 |
| Authenticator | `/authenticator` | `PREVIEW` | 否 | 仍保留恢复链路，不做独立首发产品。 |
| SimpleLogin | `/simplelogin` | `PREVIEW` | 否 | `Labs`。 |
| Standard Notes | `/standard-notes` | `PREVIEW` | 否 | `Labs`。 |
| VPN | `/vpn` | `PREVIEW` | 否 | `Labs`。 |
| Meet | `/meet` | `PREVIEW` | 否 | `Labs`。 |
| Wallet | `/wallet` | `PREVIEW` | 否 | `Labs`。 |
| Lumo | `/lumo` | `PREVIEW` | 否 | `Labs`。 |
| Collaboration / Command Center / Notifications | `/collaboration` `/command-center` `/notifications` | `PREVIEW` | 否 | 只保留方向验证，不进入首发承诺。 |

## 管理规则
- `GA`：首发承诺能力，必须进入 release gate。
- `BETA`：可见、可用，但不作为首发阻塞。
- `PREVIEW`：默认导航隐藏，只能通过 `Labs` 或显式调试路径进入。
