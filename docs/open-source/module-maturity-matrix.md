# Module Maturity Matrix

**版本**: `v1.6-mainline`
**日期**: `2026-04-09`
**作者**: `Codex`

| 模块 | 路由 / 入口 | 成熟度 | 默认导航 | 当前说明 |
|---|---|---|---|---|
| Auth / Session / MFA | `/login` `/register` `/security` | `GA` | 否 | 已纳入 release-blocking 与安全门禁。 |
| Mail | `/inbox` | `GA` | 是 | 主路径稳定；`v1.6` 延续正文 / 草稿 / 附件 E2EE、Web Push inbox 下发、最小 external SMTP outbound，以及外部密码保护安全投递的附件闭环、草稿恢复与公开页本地解密下载。 |
| Calendar | `/calendar` | `GA` | 是 | 维持稳定 GA 工作流；已补内部邀请编排与 RSVP 状态回投。 |
| Drive | `/drive` | `GA` | 是 | 维持稳定 GA 工作流；已交付 owner 文件主链客户端 E2EE foundation 与单文件 `readable-share` E2EE foundation。 |
| Suite Shell | `/suite` | `GA` | 是 | 统一工作区壳层与入口；`v1.6` 起按 `Overview / Plans / Billing / Operations / Boundary` 分区收口。 |
| Business / Admin | `/business` `/organizations` | `GA` | 是 | 租户、RBAC、审计和治理入口。 |
| Settings / Security / System Health | `/settings` `/security` | `GA` | 是 | 包含可观测性、安全、PWA readiness、Mail E2EE foundation、adoption readiness 与 release boundary map。 |
| Docs | `/docs` | `BETA` | 是 | 保留为单人 / 轻协作，不作为正式发布阻塞。 |
| Sheets | `/sheets` | `BETA` | 是 | 保留为单人 / 轻协作，不作为正式发布阻塞。 |
| Billing | `Suite > Billing center` | `BETA` | 否 | Community 仅承载报价 / 草稿 / 账单状态，不承诺真实支付闭环。 |
| Pass | `/pass` | `BETA` | 是 | 已进入默认导航，作为主线可见 Beta 入口；当前只覆盖个人 / 共享工作台、mailbox / alias、secure link、monitor 等已验证主路径，不进入 home fallback，也不作为正式发布阻塞。 |
| Authenticator | `/authenticator` | `PREVIEW` | 否 | `Labs`。 |
| SimpleLogin | `/simplelogin` | `PREVIEW` | 否 | `Labs`。 |
| Standard Notes | `/standard-notes` | `PREVIEW` | 否 | `Labs`。 |
| VPN | `/vpn` | `PREVIEW` | 否 | `Labs`；`v1.6` 默认 curated Labs catalog 不再展示。 |
| Meet | `/meet` | `PREVIEW` | 否 | `Labs`；`v1.6` 默认 curated Labs catalog 不再展示。 |
| Wallet | `/wallet` | `PREVIEW` | 否 | `Labs`；`v1.6` 默认 curated Labs catalog 不再展示。 |
| Lumo | `/lumo` | `PREVIEW` | 否 | `Labs`；`v1.6` 默认 curated Labs catalog 不再展示。 |
| Collaboration / Command Center / Notifications | `/collaboration` `/command-center` `/notifications` | `PREVIEW` | 否 | 只保留方向验证，不进入正式发布承诺；`v1.6` 默认 curated Labs catalog 不再展示。 |

## 管理规则
- `GA`：正式承诺能力，必须进入 release gate。
- `BETA`：可见、可用，但不作为正式发布阻塞；可位于默认导航或 `Suite`，若仍位于 `LABS` 面，则不得进入默认导航。
- `PREVIEW`：默认导航隐藏，只能通过 `Labs` 或显式调试路径进入。
