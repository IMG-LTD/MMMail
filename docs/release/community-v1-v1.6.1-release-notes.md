# Community Edition v1.6.1 Release Notes

**版本**: `v1.6.1-release-notes`  
**日期**: `2026-04-09`  
**作者**: `Codex`

## 当前状态
- 当前文档为 `v1.6.1` 公开基线说明。
- 上一个公开基线：`v1.6.0`
- 本次版本定位：在 `v1.6.0` 的产品聚焦与信息架构收口之上，继续把主线协作、采用入口和开源治理收深做实。

## Summary
- `MMMail Community Edition v1.6.1` 继续坚持“不把未交付能力包装成正式承诺”的原则。
- 本次版本没有扩更多默认模块，而是把 `Mail → Calendar → Drive → Pass` 从策略口径推进到更连续的 handoff 产品面。
- 同时收口了 adoption / developer docs / governance 入口，减少开源用户对当前版本、边界和支持承诺的误判。

## Included
- `Mainline collaboration depth`
  - `Suite` 总览补充主线旅程、handoff run、recent evidence 与 next-step
  - `Collaboration` 与 `Drive` 复用主线 handoff 视图
  - `Drive` launchpad 暴露 owner-side E2EE readiness、share readiness 与 `Pass` handoff CTA
- `Pass visible beta surface`
  - `Pass` 继续保持默认导航中的可见 `Beta` 交接面
  - 不再依赖 `Labs-only` 心智来解释主线安全交接
- `Adoption + developer handoff`
  - 浏览器内 `Adoption guide`
  - 浏览器内 `Team enablement`
  - `Identity readiness`
  - `Developer station`
  - `API docs` / `OpenAPI` / `Swagger` 统一到当前部署目标
- `Governance alignment`
  - `README`
  - `CONTRIBUTING`
  - `support boundaries`
  - `module maturity matrix`
  - `known issues`
  - `roadmap`
  - `feedback intake`
  - `threat model`
  - GitHub Issue 模板与 CI 分支触发器

## Validation Evidence
- 前端定向验证：
  - `pnpm --dir frontend exec vitest run tests/adoption-readiness-panel.spec.ts tests/api-docs-site.spec.ts tests/developer-docs-site.spec.ts`
  - `pnpm --dir frontend exec vitest run tests/labs-catalog.spec.ts`
- 全批回归验证：
  - `pnpm --dir frontend exec vitest run tests/suite-mainline-handoff-panel.spec.ts tests/suite-overview-core-workflows.spec.ts tests/collaboration-mainline-run.spec.ts tests/drive-launchpad.spec.ts tests/adoption-readiness-panel.spec.ts tests/api-docs-site.spec.ts tests/developer-docs-site.spec.ts tests/labs-catalog.spec.ts tests/org-suite-surfaces.spec.ts`
  - `pnpm --dir frontend typecheck`

## Support Boundaries
- `GA`
  - `Auth / Session / MFA`
  - `Mail`
  - `Calendar`
  - `Drive`
  - `Suite Shell / Settings / Security / System Health`
  - `Business / Admin`
- `BETA`
  - `Docs`
  - `Sheets`
  - `Billing center`
  - `Pass`
- `PREVIEW`
  - `Authenticator`
  - `SimpleLogin`
  - `Standard Notes`
  - `VPN`
  - `Meet`
  - `Wallet`
  - `Lumo`
  - `Collaboration / Command Center / Notifications`

## Known Limitations
- `Pass` 仍不承诺浏览器扩展、自动填充与真实 passkey ceremony。
- `Docs / Sheets` 仍以单人 / 轻协作为边界，不承诺官方级实时协作深能力。
- `Billing center` 仍不承诺真实支付、税费、发票下载与订阅结算。
- Community 仍未交付 `SSO / SCIM / LDAP` 自动化、`SMTP inbound / IMAP / Bridge`、原生客户端与零知识全栈邮件。
- 默认 self-hosted 安装仍偏向完整环境，不是极简单机模式。
