# Community Edition v1.6.0 Release Notes

**版本**: `v1.6.0-release-notes`
**日期**: `2026-04-08`
**作者**: `Codex`

## 当前状态
- 当前文档为 `v1.6.0` 发布收口说明。
- 上一个公开基线：`main` / `v1.5.0` / `e8dd2cd`
- 发布分支：`dev/v1.6`（已合并回 `main`）
- 本次版本定位：在 `v1.5.0` 基线之上，收窄产品面、重组主入口信息架构，并把 release boundary 说明提升到更可信的状态。
- 正式 release tag：`v1.6.0`
- 正式发布切点：`main` 上的 `v1.6.0` merge commit

## Summary
- `MMMail Community Edition v1.6.0` 不继续横向扩模块，而是把主线资源投入到产品聚焦、信息架构收口和能力边界可信度提升。
- 本次版本没有把 `SMTP inbound / IMAP / Bridge / zero-knowledge / native clients` 混进发布承诺，也没有把 `Labs` 的 raw registry 当成默认支持面。
- `v1.6.0` 的目标是让已经存在的 Community 能力被更准确地呈现、验证和发布，而不是制造新的能力幻觉。

## Included
- `Suite sectioned IA`
  - `/suite` 由单页混排改为 `Overview / Plans / Billing / Operations / Boundary` 五个分区
  - 主入口不再把计划、账单、运维和边界信息混排在一个长页里
- `Curated Labs catalog`
  - `/labs` 默认只展示 `Pass / Authenticator / SimpleLogin / Standard Notes`
  - `VPN / Meet / Wallet / Lumo / Collaboration / Command Center / Notifications` 仍保留在底层 registry，但不进入默认 curated catalog
- `Boundary + docs sync`
  - `README.md`
  - `docs/ops/install.md`
  - `docs/ops/runbook.md`
  - `docs/release/community-v1-support-boundaries.md`
  - `docs/open-source/module-maturity-matrix.md`
  - `frontend/public/self-hosted/*.html`
  - 当前前端 boundary locale 与 canonical docs path
- `Runtime a11y gate`
  - 新增关键公开面与主入口的运行时无障碍自动化校验
  - 相关 landmark 语义已补齐到 `Suite` boundary 面板与公开 mail share 页面
- `Carried-forward shipped baseline`
  - `PWA`
  - `Mail E2EE` 当前闭环
  - 外部密码保护安全投递的正文 / 附件 / 草稿恢复 / 公开页本地解密下载
  - `Drive E2EE foundation`
  - `Web Push`
  - `SMTP outbound adapter`
  - `Calendar internal invitation orchestration`
  - `Pass Beta readiness`

## Validation Evidence
- 定向前端回归：
  - `pnpm --dir frontend exec vitest run tests/suite-sections.spec.ts tests/labs-catalog.spec.ts tests/community-navigation.spec.ts tests/community-boundary.spec.ts tests/a11y-baseline.spec.ts tests/a11y-runtime.spec.ts tests/mail-public-share.spec.ts tests/pwa-settings-panel.spec.ts tests/i18n.spec.ts`
- 默认发布门禁：
  - `bash scripts/validate-local.sh`

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
  - `Pass`（仍为 `Labs only`）
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
- `/labs` 的收敛只作用于默认 curated catalog，不意味着底层 registry 已删除对应模块。
- `v1.6.0` 不新增 `SMTP inbound / IMAP / Bridge`、完整 MIME 级外部 E2EE 或零知识邮件架构。
- Community `Billing` 仍不承诺真实支付闭环。
- `Pass` 仍未交付浏览器扩展、自动填充、真实 `WebAuthn / passkey ceremony` 与暗网真实数据源。

## Deferred / Out of Scope
- 完整零知识邮件架构与搜索
- `SMTP inbound / IMAP / Bridge`
- 多设备密钥恢复 / 吊销 / 轮换
- `VPN / Meet / Wallet / Lumo` 深化
- 原生客户端
