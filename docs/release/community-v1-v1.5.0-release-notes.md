# Community Edition v1.5.0 Release Notes

**版本**: `v1.5.0-release-notes`
**日期**: `2026-04-08`
**作者**: `Codex`

## 当前状态
- 当前文档为 `v1.5.0` 发布收口说明。
- 发布基线：`main` / `v1.4.0` / `1db9c44`
- 开发分支：`dev/v1.5`
- 本次版本定位：在 `v1.4.0` 基线之上，把 `Mail` 外部密码保护安全投递从 `body-only` 最小闭环推进到真实可用闭环。
- 正式 release tag：`v1.5.0`
- 正式发布切点：`main` 上的 `v1.5.0` merge commit

## Summary
- `MMMail Community Edition v1.5.0` 聚焦一个高优先级缺口：让外部安全投递第一次覆盖正文、附件、草稿恢复与公开页 trust UX。
- 本次版本没有把 `SMTP inbound / IMAP / Bridge / zero-knowledge` 混进来，也没有把 `public secure link` 包装成完整外部 E2EE 邮件协议。
- `v1.5.0` 的重点是把现有 `Mail E2EE + SMTP outbound + public route` 拼成一个真实、可测试、边界清楚的外部安全投递闭环。

## Included
- `Mail external secure delivery closure`
  - 发件侧浏览器内加密正文与附件
  - 服务端持久化密文正文、密文附件与 secure link metadata
  - `saveDraft / reopen draft` 保留 external secure delivery 状态、密码提示与过期时间
  - 公开页面 `/share/mail/[token]` 通过密码本地解密正文，并解密下载附件
- `Composer / public-share trust UX`
  - composer 明确当前模式是 password-protected public secure link，而不是完整外部邮箱互通
  - 公开页面增加 trust boundary、附件状态与错误暴露
- `Docs / boundary alignment`
  - `README.md`
  - `docs/release/community-v1-support-boundaries.md`
  - `docs/open-source/module-maturity-matrix.md`
  - `frontend/public/self-hosted/*.html`
  - 当前前端 boundary locale 与 canonical docs 路径

## Validation Evidence
- 默认发布门禁：
  - `bash scripts/validate-local.sh`
- 后端定向集成测试：
  - `env -u http_proxy -u https_proxy -u HTTP_PROXY -u HTTPS_PROXY -u ALL_PROXY -u all_proxy bash -lc 'source scripts/lib/java-common.sh && MVN_BIN=$(resolve_maven_bin "$PWD") && timeout 60s "$MVN_BIN" -f backend/pom.xml -pl mmmail-server -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=MailExternalEncryptedDeliveryIntegrationTest,MailAttachmentIntegrationTest test'`
- 前端定向测试：
  - `env -u http_proxy -u https_proxy -u HTTP_PROXY -u HTTPS_PROXY -u ALL_PROXY -u all_proxy pnpm --dir frontend exec vitest run tests/mail-public-share.spec.ts tests/mail-compose.spec.ts tests/mail-attachments.spec.ts tests/mail-draft-e2ee.spec.ts tests/mail-smoke.spec.ts tests/mail-compose-e2ee.spec.ts tests/community-boundary.spec.ts tests/mail-compose-i18n.spec.ts tests/pwa-settings-panel.spec.ts`

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
- 外部 secure delivery 仍是 `public secure link` 模式，不是完整 MIME 级外部邮箱互通。
- SMTP 通知邮件只承担 secure link 送达，不承担明文正文或明文附件外发。
- 零知识邮件架构、`SMTP inbound / IMAP / Bridge`、外部联系人公钥发现仍未交付。

## Deferred / Out of Scope
- 完整 MIME 外部 E2EE 兼容
- `SMTP inbound / IMAP / Bridge`
- 零知识邮件架构与搜索
- 多设备密钥恢复 / 吊销 / 轮换
- Drive / Pass / Enterprise 新 tranche
