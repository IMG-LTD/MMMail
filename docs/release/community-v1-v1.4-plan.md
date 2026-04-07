# Community Edition v1.4 Planning Baseline

**版本**: `v1.4-planning-batch-1`
**日期**: `2026-04-07`
**作者**: `Codex`

## 变更记录
- `2026-04-07`：初始化 `v1.4` 规划基线，确认 `main` 已包含 `v1.3.1`（`4b442cc`）。
- `2026-04-07`：从 `main` 新开 `dev/v1.4`，冻结首批主线为 `Mail 外部密码保护加密投递`。
- `2026-04-07`：完成首批实现与定向验证，进入发布文档与边界收口阶段。

## 规划背景
- `v1.3.1` 已把 `Mail E2EE`、`Drive E2EE foundation`、`Web Push`、`SMTP outbound`、`Calendar invite orchestration` 与 `Pass Beta readiness` 收口到可验证状态。
- 但 `Mail` 仍缺一个高优先级的真实落差：对外部收件人只能走最小 `SMTP outbound`，无法提供任何受密码保护的加密正文投递体验。
- `v1.4` 不再扩产品宽度，而是在当前已存在的 `OpenPGP + SMTP outbound + public route` 基础上，补一个真实可用、边界清晰的外部加密邮件最小闭环。

## `v1.4` 核心目标
1. 交付 `Mail` 外部密码保护加密投递的最小真实闭环。
2. 保持服务端只保存外部 secure delivery 的密文正文，不把明文正文塞入通知邮件。
3. 通过公开 secure link 页面在浏览器本地完成密码解密，不引入 silent fallback。
4. 把 release 文档、README、运维指引与前端边界文案统一到 `v1.4` 真实能力面。

## 批次原则
- 只做当前技术栈里可验证、可回归的 tranche。
- 不把 `SMTP inbound / IMAP / Bridge / zero-knowledge` 混入当前批次。
- 不用 mock success、不加静默兜底；失败必须显式暴露。

## `v1.4` 工作流拆分
| Stream | 主题 | 优先级 | 当前决策 | 说明 |
|---|---|---|---|---|
| A | `Mail external password-protected encrypted delivery` | `P0` | `Done` | 已完成：浏览器加密正文、服务端密文存储、SMTP 通知邮件、secure link 公开页面本地解密已打通。 |
| B | `Release docs and capability boundary alignment` | `P0` | `Done` | 已完成：README、ops docs、release docs、boundary locale 与 maturity matrix 已收口到 `v1.4`。 |
| C | `Final targeted validation` | `P0` | `Ready` | 只运行与当前 tranche 直接相关的前后端定向验证。 |
| D | `External encrypted attachments` | `P1` | `Backlog` | 当前未做。需要附件 envelope 与公开页面下载 / 解密链扩展。 |
| E | `External encrypted drafts` | `P1` | `Backlog` | 当前未做。需要把 external secure-delivery intent 引入 draft 契约。 |
| F | `SMTP inbound / IMAP / Bridge` | `P2` | `Backlog` | 继续留在协议栈路线图，不进入本批。 |
| G | `Zero-knowledge mail architecture` | `P2` | `Backlog` | 继续留在路线文档，不在 `v1.4` 承诺内。 |

## 已批准范围
### 1. Mail 外部密码保护加密投递
- 只支持外部收件人的 `body-only secure delivery`。
- 发件侧在浏览器内完成正文加密，并同时使用收件分享密码与 sender public key 构建外部密文载荷。
- 服务端保存外部密文正文与 secure link metadata；`SMTP outbound` 只外发通知邮件与 secure link，不外发正文。
- 公开页面 `/share/mail/[token]` 先读取公开 metadata，再在浏览器内基于密码本地解密正文。
- 缺少 `SMTP` 配置、public base URL 或密文载荷时，必须返回真实错误，不做 silent fallback。

### 2. 文档与边界收口
- `README.md`、`docs/ops/install.md`、`docs/ops/runbook.md`、`docs/release/community-v1-support-boundaries.md`、`docs/open-source/module-maturity-matrix.md` 必须反映 `v1.4` 的真实边界。
- 前端 `Release boundary map`、`Mail E2EE foundation`、`PWA readiness`、`Adoption readiness` 文案必须同步到 `v1.4`。
- 新增 `v1.4` 规划基线、主线路线与 release notes 文档。

## 明确不进入 `v1.4`
1. 外部加密附件
2. 外部加密草稿
3. secure link 附件下载 / 本地解密
4. 完整 MIME 外部 E2EE 兼容
5. `SMTP inbound / IMAP / Bridge`
6. 零知识邮件架构
7. 原生客户端
8. Preview 模块默认主线深化

## 完成判定
- 外部收件人发信可生成 password-protected secure delivery，SMTP 外发为 secure link 通知，而不是明文正文。
- 公开页面可以基于密码在浏览器本地解密正文。
- 缺失公开路由放行、`SMTP` 配置或密文载荷时，错误路径显式可见。
- 定向前后端测试通过，且 README / release docs / boundary docs / locale 不再停留在 `v1.3` 口径。
