# Community Edition v1.4 Mainline Roadmap

**版本**: `v1.4-mainline-roadmap-batch-1`
**日期**: `2026-04-07`
**作者**: `Codex`

## 变更记录
- `2026-04-07`：初始化 `v1.4` 主线路线，并确认 `main` 已包含 `v1.3.1` 基线。
- `2026-04-07`：冻结首批为 `Mail 外部密码保护加密投递`。
- `2026-04-07`：完成首批实现、测试与边界文档同步。

## 主线结论
- 当前公开基线：`main` → `v1.3.1`（`4b442cc`）
- 当前开发分支：`dev/v1.4`
- `v1.4` 不再追求横向扩模块，而是在 `Mail` 上补齐一个最靠近用户价值的外部加密最小闭环。

## 主线原则
- 继续把“开源、自托管、具备真实 E2EE 能力的协作套件”作为主定位。
- 只推进当前仓库可验证、可回归、可诚实声明的 tranche。
- 不把 body-only secure delivery 写成完整外部 E2EE 邮件协议。

## `v1.4` 主线路线
| Lane | 主题 | 状态 | 说明 |
|---|---|---|---|
| L1 | `Mail external password-protected encrypted delivery` | `Completed` | 已完成：浏览器加密正文、服务端密文存储、SMTP secure-link 通知与公开页面本地解密闭环。 |
| L2 | `Release docs and boundary sync` | `Completed` | 已完成：README、ops docs、release docs、boundary locale 与 maturity matrix 已切到 `v1.4` 口径。 |
| L3 | `Final targeted validation` | `Ready` | 当前只需要跑前后端定向验证并确认 `v1.4` 收口。 |
| L4 | `External encrypted attachments` | `Backlog` | 当前未做；需要 content envelope 扩展到附件。 |
| L5 | `External encrypted drafts` | `Backlog` | 当前未做；draft 契约与 reopen UI 还未覆盖该模式。 |
| L6 | `SMTP inbound / IMAP / Bridge` | `Backlog` | 继续在协议栈路线图中排队。 |
| L7 | `Zero-knowledge mail architecture` | `Backlog` | 继续在路线文档中排队。 |

## Batch 1
- `Mail external password-protected encrypted delivery`

## Batch 1 产出结果
- 后端：
  - 新增外部 secure link 数据模型、mapper、service 与公开 controller。
  - `MailService` / `MailE2eeMessageService` / recipient discovery 主链支持外部 secure delivery payload。
  - `SecurityConfig` 与 observability route resolver 已放行 `/api/v1/public/mail/**`。
  - 新增迁移：`V8__mail_external_secure_links.java`。
- 前端：
  - `MailComposer` 允许在外部收件场景构建 password-protected external encrypted payload。
  - 新增 `/share/mail/[token]` 公开页面，通过密码在浏览器本地解密正文。
  - 新增 `public-mail` 布局、公开 mail API util 与三语 locale。
- 测试：
  - 后端新增 `MailExternalEncryptedDeliveryIntegrationTest`。
  - 前端新增 `mail-public-share.spec.ts`，并扩展 composer E2EE / i18n 定向测试。

## Batch 1 当前边界
- 只覆盖 `body-only secure delivery`。
- 只覆盖发信后的 secure link 阅读，不覆盖外部加密草稿。
- SMTP 外发只承担通知邮件角色，不承担完整加密 MIME 传输。
- 不承诺外部加密附件、外部联系人公钥体系、password reset 或零知识元数据。

## 明确延后
- 外部加密附件 / 附件公开页本地解密
- 外部加密草稿
- `SMTP inbound / IMAP / Bridge`
- 零知识邮件架构
- 原生客户端

## 参考
- `docs/release/community-v1-v1.4-plan.md`
- `docs/release/community-v1-support-boundaries.md`
- `docs/open-source/module-maturity-matrix.md`
- `docs/architecture/mail-zero-knowledge-roadmap.md`
