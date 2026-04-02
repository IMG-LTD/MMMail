# Community Edition v1.2 Capability Boundaries

**版本**: `v1.2-boundary-refresh`
**日期**: `2026-04-02`
**作者**: `Codex`

## 当前分支状态
- `release/v1.0`：继续承接 `v1.0.x` 维护与 backport。
- `dev/community-v1`：`v1.1.0` 发布后转入 `v1.2` 集成开发。
- 本文档只描述当前 `v1.2` 开发主线的真实能力边界，不回写历史 `v1.0.x` 发布承诺。

## 对外口径原则
- 只描述仓库中可验证的实现，不把 future design、架构预研或 UI 骨架表述为“已上线能力”。
- `Community Edition` 当前基线是 `Web-first`、`self-hosted collaboration suite`，不是零知识邮件服务。
- 若某项能力仍处于 `Discovery`，则必须先完成 ADR、威胁模型更新和迁移设计，再进入发布承诺。
- 若某项能力只完成主路径而未覆盖完整产品面，必须显式标注为 `Limited`，不能直接写成 `Implemented`。

## `v1.2` 能力状态
| 能力 | 当前状态 | 说明 |
|---|---|---|
| PWA 壳层 | `Implemented` | 已交付 `manifest.webmanifest`、`Service Worker` 注册、安装入口与离线壳层入口。 |
| Web Push 下发 | `Not shipped` | 当前只暴露浏览器权限准备状态，未交付可验证的推送下发链路。 |
| 原生客户端 | `Not shipped` | 尚未交付 `iOS / Android / Desktop` 原生客户端，当前主线仍是 `Web-first`。 |
| Mail E2EE | `Limited` | 已交付 key profile foundation、recipient readiness、`READY` 内部路由正文加密发送与详情页本地解密；附件、草稿、外部收件人、Drive 与零知识架构仍未交付。 |
| Drive E2EE | `Discovery` | 当前仓库未发现可验证的 Drive 客户端加密上传或零知识文件模型实现。 |
| Zero-knowledge mail architecture | `Discovery` | `v1.2` 已补演进路线文档，但仓库尚未交付服务端无法读取用户邮件内容的零知识架构。 |
| SMTP / IMAP / Bridge | `Discovery` | `v1.2` 已补协议栈 discovery 文档，但仍未交付可验证的外部 SMTP、IMAP 或 Bridge 能力。 |
| OpenAPI / self-hosted adoption docs | `Implemented` | 设置页已暴露 `Swagger UI`、`OpenAPI JSON` 与内置 install / runbook 快速页，自托管采用者可直接校验当前 `v1.2` 主线。 |
| Real billing / payment capture | `Hosted-only` | Community 仅保留报价 / 账单状态可见性，不承诺真实支付扣款。 |

## Community 与 Hosted 边界
### Community
- 自托管部署、升级、备份恢复与基础协作工作区。
- `Mail / Calendar / Drive / Admin / Workspace Shell` 为当前主线中可见的稳定面。
- `Docs / Sheets / Billing center` 维持 `Beta`，允许能力深度不完整。
- `Mail E2EE` 当前属于受限交付：只覆盖已实现主路径，不外推为完整零知识邮件系统。
- 自托管采用入口已提供文档与 API reference 暴露，但这不等于公共开发者平台、托管 onboarding 或 Hosted 支持承诺已经交付。

### Hosted / Commercial
- 真实支付扣款、商业订阅生命周期、税费 / 发票 / 对账。
- 商业 SLA、托管运维与 Hosted 支持承诺。
- 若未来承诺 `E2EE / 零知识 / Bridge` 的 hosted 版本，也必须单独完成架构与安全评审后再公开承诺。

## 明确不应被误解为已完成的事项
- `Meet / VPN / Wallet / Lumo / Pass` 的 `Preview` 或 `Labs` UI，不等于其深层引擎已具备正式发布条件。
- `PWA` 已交付不等于 `Web Push`、离线写入同步或原生客户端已交付。
- `Mail E2EE` 已交付的只是 foundation + recipient readiness + `READY` 内部路由正文加密，不等于附件、草稿、Drive 或零知识架构已完成。
- `Mail Easy Switch` 的文件导入链路不等于 `OAuth / IMAP` 直连迁移已经可用。

## 单一事实来源
- `README.md`
- `docs/release/community-v1-v1.2-mainline-roadmap.md`
- `docs/release/community-v1-v1.2-capability-boundaries.md`
- `docs/architecture/mail-zero-knowledge-roadmap.md`
- `docs/architecture/mail-protocol-stack-discovery.md`
- `docs/open-source/module-maturity-matrix.md`
- `/suite` 中的 `Release boundary map`
