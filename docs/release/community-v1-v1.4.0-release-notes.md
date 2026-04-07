# Community Edition v1.4.0 Release Notes

**版本**: `v1.4.0-release-notes`
**日期**: `2026-04-07`
**作者**: `Codex`

## 当前状态
- 当前文档为 `v1.4.0` 发布收口说明。
- 发布基线：`main` / `v1.3.1` / `4b442cc`
- 开发分支：`dev/v1.4`
- 本次版本定位：在 `v1.3.1` 基线之上补齐 `Mail` 外部密码保护加密投递最小闭环。
- 正式 release tag：`v1.4.0`
- 正式发布切点：`main` 上的 `v1.4.0` merge commit

## Summary
- `MMMail Community Edition v1.4.0` 聚焦一个高优先级差距：让外部收件人第一次拥有受密码保护的加密正文阅读路径。
- 本次版本没有把 `SMTP inbound / IMAP / Bridge / zero-knowledge` 混进来，也没有把 `body-only secure delivery` 包装成完整外部 E2EE 邮件协议。
- `v1.4.0` 的重点是把现有 `Mail E2EE + SMTP outbound + public route` 拼成一个真实、可测试、边界清楚的外部加密最小能力。

## Included
- `Mail external password-protected encrypted delivery`
  - 发件侧浏览器内加密正文
  - 服务端持久化密文正文与 secure link metadata
  - `SMTP outbound` 外发 secure link 通知邮件
  - 公开页面 `/share/mail/[token]` 通过密码本地解密正文
- `Security / routing hardening`
  - 显式放行 `/api/v1/public/mail/**`
  - 公开 mail 路由纳入 request route resolution
- `Docs / boundary alignment`
  - `README.md`
  - `docs/ops/install.md`
  - `docs/ops/runbook.md`
  - `docs/release/community-v1-support-boundaries.md`
  - `docs/open-source/module-maturity-matrix.md`
  - 前端 boundary locale 与 canonical docs 路径

## Validation Evidence
- 默认发布门禁：
  - `bash scripts/validate-local.sh`
- 后端编译：
  - `mvn -pl mmmail-server -DskipTests compile`
- 后端定向集成测试：
  - `mvn -pl mmmail-server -Dtest=MailExternalEncryptedDeliveryIntegrationTest,SmtpOutboundDeliveryIntegrationTest,MailE2eeMessageEncryptionIntegrationTest test`
- 前端定向测试：
  - `pnpm exec node ./scripts/nuxt-command.mjs prepare --logLevel silent && pnpm exec vitest run tests/mail-compose-e2ee.spec.ts tests/mail-compose-i18n.spec.ts tests/mail-public-share.spec.ts`

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
- 外部 secure delivery 当前只覆盖正文，不覆盖附件。
- 外部 secure delivery 当前不覆盖草稿 reopen 语义。
- SMTP 通知邮件只承担 secure link 送达，不承担完整加密 MIME 投递。
- 零知识邮件架构、`SMTP inbound / IMAP / Bridge` 仍未交付。

## Deferred / Out of Scope
- 外部加密附件
- 外部加密草稿
- 完整 MIME 外部 E2EE 兼容
- `SMTP inbound / IMAP / Bridge`
- 零知识邮件架构
- 原生客户端
