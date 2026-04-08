# Community Edition v1.5 Planning Baseline

**版本**: `v1.5-planning-batch-1`
**日期**: `2026-04-08`
**作者**: `Codex`

## 变更记录
- `2026-04-08`：初始化 `v1.5` 规划基线，确认 `main` 已包含 `v1.4.0`（`1db9c44`）。
- `2026-04-08`：从 `main` 新开 `dev/v1.5`，冻结首批主线为 `Mail external secure delivery closure`。

## 规划背景
- `v1.4.0` 已完成外部 password-protected encrypted delivery 的 `body-only` 最小闭环。
- 当前主缺口不再是“有没有外部安全投递”，而是“闭环是否可信”：附件裸缺失、草稿 reopen 不完整、公开阅读页只覆盖正文。
- `v1.5` 继续收窄范围，不扩产品宽度，只把 `Mail` 外部安全投递推进到一个更接近真实使用的 tranche。

## `v1.5` 核心目标
1. 交付外部安全邮件附件闭环：发送、存储、公开分享页本地解密下载。
2. 交付外部安全邮件草稿恢复闭环：保存后重新打开可以恢复安全投递状态。
3. 提升公开分享页和 composer 的信任体验、边界声明与显式错误暴露。
4. 统一 `README`、release docs、support boundaries、maturity matrix 到 `v1.5` 真实能力面。

## 批次原则
- 只做当前技术栈里可验证、可回归、可诚实声明的 tranche。
- 不把 `SMTP inbound / IMAP / Bridge / zero-knowledge metadata / key recovery` 混入当前批次。
- 不做 silent fallback，不做 mock success，不把“附件 secure delivery”写成完整 MIME E2EE 协议。

## `v1.5` 工作流拆分
| Stream | 主题 | 优先级 | 当前决策 | 说明 |
|---|---|---|---|---|
| A | `Mail external secure attachments` | `P0` | `Done` | 已完成：复用现有 attachment pipeline，把密文附件带入 secure link 公开读取链。 |
| B | `Mail external secure draft reopen` | `P0` | `Done` | 已完成：external secure delivery intent、密码提示与附件元数据已纳入草稿 reopen。 |
| C | `Public secure mail trust UX` | `P0` | `Done` | 已完成：公开页补齐附件状态、密码提示、失败暴露与信任文案。 |
| D | `Composer progressive disclosure` | `P1` | `Done` | 已完成：composer 明确告知外部 secure delivery 边界、附件行为与公共链接体验。 |
| E | `Release docs and capability boundary alignment` | `P0` | `Done` | 已完成：`README`、ops docs、release docs、locale、maturity matrix 已切到 `v1.5`。 |
| F | `Drive collaborator decrypt` | `P2` | `Backlog` | 不进入本批。 |
| G | `Pass extension / autofill` | `P2` | `Backlog` | 不进入本批。 |
| H | `SSO / SCIM / Enterprise` | `P3` | `Backlog` | 不进入本批。 |

## 已批准范围
### 1. Mail 外部安全附件
- external secure delivery 允许附带附件。
- 服务端保存附件关联与密文元数据，不把明文附件拼进 SMTP 通知邮件。
- 公开页面获取附件清单，并在浏览器本地完成解密后下载。

### 2. Mail 外部安全草稿恢复
- `saveDraft` 持久化 external secure delivery intent、密码提示、附件元数据与 E2EE 载荷。
- reopen draft 时恢复 composer UI，不允许 silently 丢状态。

### 3. Trust UX 与文档收口
- 公开分享页明确显示 `password-protected secure delivery`、附件状态、过期与错误路径。
- composer 明确显示当前模式是 public secure link，而不是完整端到端互通邮箱。
- `README`、release docs、runbook、support boundaries 与 locale 文案同步更新。

## 明确不进入 `v1.5`
1. `SMTP inbound`
2. `IMAP / Bridge`
3. 零知识元数据与搜索
4. 外部联系人公钥发现
5. 多设备密钥恢复 / 吊销 / 轮换
6. Drive / Docs / Sheets / Pass 新 tranche
7. Preview 模块裁剪重构

## 完成判定
- 外部安全邮件支持附件加密发送、公开页本地解密下载。
- 外部安全邮件草稿可恢复，而不是 reopen 后退化为普通邮件。
- 前后端定向测试通过，`bash scripts/validate-local.sh` 通过。
- `README`、boundary docs、release docs 与 maturity matrix 不再停留在 `v1.4` 口径。
