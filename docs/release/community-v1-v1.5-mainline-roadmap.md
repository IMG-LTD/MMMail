# Community Edition v1.5 Mainline Roadmap

**版本**: `v1.5-mainline-roadmap-batch-1`
**日期**: `2026-04-08`
**作者**: `Codex`

## 变更记录
- `2026-04-08`：初始化 `v1.5` 主线路线，并确认 `main` 已包含 `v1.4.0` 基线。
- `2026-04-08`：冻结首批为 `Mail external secure delivery closure`。

## 主线结论
- 当前公开基线：`main` → `v1.4.0`（`1db9c44`）
- 当前开发分支：`dev/v1.5`
- `v1.5` 继续把主线资源集中在 `Mail`，目标不是新增更多模块，而是让外部安全投递从 `body-only demo` 走向更可信的用户闭环。

## 主线原则
- 继续把“开源、自托管、具备真实 E2EE 能力的协作套件”作为主定位。
- 优先补齐最靠近用户价值、最容易被实际使用卡住的闭环缺口。
- 保持能力声明诚实：只声明当前仓库已经真实交付并可回归验证的能力。

## `v1.5` 主线路线
| Lane | 主题 | 状态 | 说明 |
|---|---|---|---|
| L1 | `Mail external secure attachments` | `Completed` | 已完成：public secure delivery 增加附件闭环。 |
| L2 | `Mail external secure draft reopen` | `Completed` | 已完成：external secure draft reopen 与状态恢复。 |
| L3 | `Public secure mail trust UX` | `Completed` | 已完成：公开页增加附件、状态与信任说明。 |
| L4 | `Release docs and boundary sync` | `Completed` | 已完成：`README`、release docs、support boundaries、maturity matrix 已同步切到 `v1.5`。 |
| L5 | `Drive collaborator decrypt` | `Backlog` | 保持在后续路线，不进入当前批次。 |
| L6 | `Pass browser extension` | `Backlog` | 保持在后续路线，不进入当前批次。 |
| L7 | `SSO / SCIM / Enterprise lane` | `Backlog` | 保持在后续路线，不进入当前批次。 |

## Batch 1
- `Mail external secure delivery closure`

## Batch 1 目标结果
- 后端：
  - external secure delivery 允许附件进入 payload 与公开读取模型。
  - secure link 公开接口返回附件密文元数据。
  - draft reopen 契约保留 external secure delivery 状态。
- 前端：
  - composer 支持 external secure attachment 发送与草稿恢复。
  - `/share/mail/[token]` 支持附件列表、本地解密与下载。
  - 公开页与 composer 文案明确当前能力边界与错误路径。
- 测试：
  - 扩展 `MailExternalEncryptedDeliveryIntegrationTest`
  - 更新 `mail-public-share.spec.ts`
  - 更新 compose / attachment 定向测试

## Batch 1 当前边界
- 仍是 public secure link 模式，不是完整外部邮箱协议互通。
- 仍不覆盖外部联系人公钥体系或 MIME 级端到端互通。
- SMTP 外发仍只承担通知邮件角色，不外发明文正文或明文附件。

## 明确延后
- `SMTP inbound / IMAP / Bridge`
- 零知识元数据与搜索
- 密钥恢复 / 吊销 / 轮换
- Drive / Pass / Enterprise 新 tranche

## 参考
- `docs/release/community-v1-v1.5-plan.md`
- `docs/release/community-v1-support-boundaries.md`
- `docs/open-source/module-maturity-matrix.md`
- `docs/architecture/mail-zero-knowledge-roadmap.md`
