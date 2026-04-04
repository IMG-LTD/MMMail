# Community Edition v1.3 Mainline Roadmap

**版本**: `v1.3-mainline-roadmap-batch-12`
**日期**: `2026-04-03`
**作者**: `Codex`

## 变更记录
- `2026-04-02`：初始化 `v1.3` 主线路线，冻结首个实现批次与排队项。
- `2026-04-02`：完成 `Mail E2EE` 内容层三批次收敛，冻结第三批为 `Drive E2EE foundation`。
- `2026-04-02`：完成 `Drive E2EE foundation`，冻结第四批为 `Web Push`。
- `2026-04-02`：完成第四批 `Web Push`，主线回到下一批次选择状态。
- `2026-04-02`：冻结第五批为 `SMTP outbound adapter`，只交付最小真实外发能力。
- `2026-04-02`：完成第五批 `SMTP outbound adapter`，并通过 `Web Push + SMTP` 组合回归验证。
- `2026-04-02`：冻结第六批为 `Calendar internal invitation orchestration`，只交付内部邀请编排与 RSVP 状态同步。
- `2026-04-02`：完成第六批 `Calendar internal invitation orchestration`，并通过后端定向回归与前端 smoke/i18n/typecheck 验证。
- `2026-04-02`：冻结第七批为 `Pass Beta readiness`，只做成熟度升级所需的最小验证闭环与边界收紧。
- `2026-04-02`：完成第七批 `Pass Beta readiness`，并通过前端定向回归、typecheck 与后端 release-blocking 集成验证。
- `2026-04-03`：冻结第八批为 `a11y baseline`，只补默认 shell 与 `Pass` 关键面板的可访问性基线。
- `2026-04-03`：完成第八批 `a11y baseline`，默认 shell 与 `Pass` 关键面板已补最小可访问性基线并通过前端定向验证。
- `2026-04-03`：冻结第九批为 `公开 API 文档站`，只补浏览器内 API 文档入口与当前主线运维文档对齐。
- `2026-04-03`：完成第九批 `公开 API 文档站`，设置页、静态 quick page 与运维文档已对齐并通过前端定向验证。
- `2026-04-03`：冻结第十批为 `i18n 运维化`，只补 placeholder 一致性门禁、独立脚本与 CI 可见性。
- `2026-04-03`：完成第十批 `i18n 运维化`，placeholder 一致性门禁、独立脚本与 CI summary 已通过定向验证。
- `2026-04-03`：完成第十一批 `组件 / composables i18n 裸字符串治理`，suite / security / organizations operator surfaces 已完成定向扫描收口并通过定向验证。
- `2026-04-03`：冻结第十二批为 `Drive readable-share E2EE foundation`，只交付单文件 public share 的可读解密基础，不把 collaborator / folder descendants 混入当前 tranche。

## 主线结论
- 当前正式版本：`v1.2.0`
- 当前主线分支：`dev/community-v1`
- `v1.3` 暂未切 release 分支，所有已批准开发先在 `dev/community-v1` 落地。

## 主线原则
- 继续把“第一个具备端到端加密能力的开源自托管协作套件”作为核心定位。
- 先补深度，不补宽度。
- 不把 `Discovery / Queued / Backlog` 项写成 `Implemented`。

## `v1.3` 主线路线
| Lane | 主题 | 状态 | 说明 |
|---|---|---|---|
| L1 | `Mail E2EE 草稿加密` | `Completed` | 第一批已完成并通过定向验证。 |
| L2 | `Mail E2EE 密钥恢复` | `Completed` | 第一批已完成并通过定向验证。 |
| L3 | `Mail E2EE 附件加密` | `Completed` | 第二批已完成并通过定向验证。 |
| L4 | `Drive E2EE foundation` | `Completed` | 第三批已完成并通过定向验证。 |
| L5 | `Web Push` | `Completed` | 第四批已完成：PWA 真实订阅与 `MAIL_INBOX` 新邮件下发已打通。 |
| L6 | `SMTP outbound adapter` | `Completed` | 第五批已完成并通过定向验证：外部收件人最小真实外发能力已打通。 |
| L7 | `Calendar internal invitation orchestration` | `Completed` | 第六批已完成：attendee → internal invite/share → RSVP 状态投影已打通。 |
| L8 | `Pass Beta readiness` | `Completed` | 第七批已完成：`PASS` 升级为 `BETA`，但继续保留 `Labs` 入口。 |
| L9 | `a11y baseline` | `Completed` | 第八批已完成：默认 shell 与 `Pass` 关键面板已具备最小可访问性基线。 |
| L10 | `公开 API 文档站` | `Completed` | 第九批已完成：浏览器内 API 文档站、设置页入口与主线自托管文档已对齐。 |
| L11 | `i18n 运维化` | `Completed` | 第十批已完成：placeholder 一致性门禁、独立脚本与 CI 独立可见面已落地。 |
| L12 | `组件 / composables i18n 裸字符串治理` | `Completed` | 第十一批已完成：suite / security / organizations operator surfaces 裸字符串扫描与三语收口已通过验证。 |
| L13 | `Drive readable-share E2EE foundation` | `Completed` | 第十二批已完成并通过定向验证：单文件 public share password-protected share ciphertext、本地解密与 `shared-with-me` reopen 已收口。 |
| L14 | `剩余 i18n 运维化增强` | `Backlog` | 更广覆盖的 components / composables 裸字符串扫描和更强发布门禁仍未落地。 |
| L15 | `Drive collaborator / folder readable-share E2EE` | `Backlog` | collaborator 公钥分发、folder descendants 与批量分享仍需更重的内容密钥分发 tranche。 |

## 已完成实现批次
### Batch 1
- `Mail E2EE draft encryption`
- `Mail E2EE key recovery`

### Batch 2
- `Mail E2EE attachment encryption`

### Batch 3
- `Drive E2EE foundation`

### Batch 4
- `Web Push`

## Batch 4 产出结果
- 后端：新增 owner 级 `Web Push` 订阅持久化与注册 / 删除 API；使用显式配置的 `VAPID` 公私钥进行真实下发；新邮件入站复制成功后触发最小推送事件。
- 前端：在现有 `PWA` 设置面板中接通通知授权后的真实订阅 / 退订链路，并持久显示订阅状态。
- Service Worker：处理 `push` 与 `notificationclick` 事件，点击通知后聚焦或打开 `Inbox` / 对应邮件路由。
- 边界：第四批只承诺 `Mail inbox` 新邮件推送，不混入 `SMTP outbound`、通用通知中心聚合推送或离线写入能力。
- 测试：新增定向前后端验证，不依赖大范围回归。

## Batch 5 产出结果
- 后端：新增独立 `SMTP outbound` 投递适配器与显式配置，支持将外部收件人作为真实投递目标，不再把所有非本地地址统一判定为不可达。
- 投递模型：复用现有 `OUTBOX / SCHEDULED` 语义；到期派发时对外部目标走 `SMTP`，对本地目标继续走当前 inbox copy 语义。
- Mail 主链：最小支持直接外部收件人发信；发送成功后保留当前 `SENT` 语义，失败时抛出真实错误，不做 silent fallback。
- 边界：第五批不交付 `SMTP inbound`、`IMAP`、`Bridge`、`SPF / DKIM / DMARC` 域名校验、外部 E2EE 邮件、外部附件兼容矩阵。
- 测试：新增后端定向集成测试，验证外部地址不再被当前路由拒绝、`SMTP` 配置缺失时报显式错误，且第四批 `Web Push` 行为未被打坏。

## 已完成第七批
### Batch 7
- `Pass Beta readiness`

### Batch 7 产出结果
- 后端：补齐 `PassReleaseBlockingIntegrationTest` 并修复旧 `Pass` 集成测试的 `test` profile 漏配。
- 前端：补齐 `pass-smoke.spec.ts` 与共享 smoke fixture，覆盖 `/pass`、`/pass-monitor` 与公开 secure link 主路径。
- 成熟度：`PASS` 从 `PREVIEW` 升级为 `BETA`，但继续保留 `surface = LABS`，不进入默认导航和 home route candidate。
- 边界文档：明确 `Pass` 当前不包含浏览器扩展、自动填充、真实 `WebAuthn / passkey ceremony`、暗网真实数据源。
- 测试：第七批已通过定向后端集成测试、前端 smoke / pass 回归与 `pnpm typecheck`。

## 已完成第八批
### Batch 8
- `a11y baseline`

### Batch 8 产出结果
- 默认 shell：已补显式 `skip link`、主导航与主内容 landmark、搜索框与安全入口可读标签，键盘用户可直接跳转到主内容。
- `Pass` 工作台：个人 / 共享 sidebar 卡片已补当前选中态与可读标签，工作台标题与空状态已接入三语 locale。
- 公开 `Pass secure link` 页面：已补 `aria-busy`、摘要区 / 敏感信息区标签与主内容跳转锚点，不改变现有视觉结构。
- 测试：新增 `frontend/tests/a11y-baseline.spec.ts`，并通过与 `pass-smoke`、`i18n`、`typecheck` 组合验证。
- 边界：第八批仍不承诺全仓语义改造、不引入浏览器自动化 a11y 扫描平台、不重做视觉样式系统。

## 已完成第九批
### Batch 9
- `公开 API 文档站`

### Batch 9 产出结果
- 新增浏览器内 `API docs` quick page，聚合 `Swagger UI`、`OpenAPI JSON`、install / runbook，并在缺少 `apiBase` 时显式提示用户输入后端地址。
- 设置页 `Adoption readiness` 面板继续保留后端直达链接，同时新增基于当前 `NUXT_PUBLIC_API_BASE` 生成的 API quick page 入口。
- 前端内置 install / runbook quick page 与仓库 `docs/ops/install.md`、`docs/ops/runbook.md` 已同步到当前 `v1.3` 主线能力边界。
- 测试：已通过 `tests/adoption-readiness-panel.spec.ts`、`tests/api-docs-site.spec.ts`、`tests/i18n.spec.ts`、`tests/i18n-governance.spec.ts` 与 `pnpm typecheck`。

### Batch 9 明确不包含
- 新增 SDK、公共开发者平台或 Hosted developer portal
- 扩展后端 API surface、变更现有 springdoc 暴露方式
- 引入新的认证模型、第三方 API 网关或对外稳定版本承诺

## 已完成第十批
### Batch 10
- `i18n 运维化`

### Batch 10 产出结果
- 在现有 locale key 对齐门禁上新增 placeholder 集合一致性校验；不一致时报告脚本和测试都会显式失败。
- `frontend` 现已提供独立 `i18n:test`、`i18n:catalog`、`i18n:coverage`、`i18n:check` 脚本，可直接作为本地与 CI 入口。
- CI 已增加独立 `Frontend i18n gates` step，并把模块数、gap 数、placeholder mismatch 数、page coverage 写入 `GITHUB_STEP_SUMMARY`。
- 测试：已通过 `pnpm i18n:check && pnpm typecheck`。

## 已完成第十一批
### Batch 11
- `组件 / composables i18n 裸字符串治理`

### Batch 11 产出结果
- surface literal gate 已收紧到冻结范围内的 7 个高价值 operator surfaces，不再把旧批次文件混入当前门禁。
- `SuiteReadinessSecurityPanel`、`SuiteCommandSearchPanel`、`SuiteGovernancePanel`、`SuiteRemediationPanel`、`SecurityAliasQuickCreate`、`OrganizationsPolicyPanel`、`useSuiteOperationsWorkspace` 当前识别出的英文 / 原始角色枚举展示已收口到三语 locale 或本地化 helper。
- 扫描规则已过滤结构性误报（如 `update:*` emits、内部 enum literal、内部 key prefix），当前 targeted gate 只对真实用户可见裸字符串失败。
- 定向验证已通过：`tests/i18n-surface-governance.spec.ts`、`tests/suite-operations-i18n.spec.ts`、`tests/org-suite-surfaces.spec.ts`、`pnpm typecheck`。

### Batch 11 明确不包含
- 全仓 `.vue` / `.ts` 通用 AST 级 i18n lint
- 新增翻译平台或自动机翻链路
- `Drive readable-share E2EE`
- Preview / Labs 模块的大面积文案收口

## 已完成第十二批
### Batch 12
- `Drive readable-share E2EE foundation`

### Batch 12 产出结果
- 后端：新增原子化 `POST /api/v1/drive/items/{itemId}/shares/e2ee` 创建流，只允许 `FILE + VIEW + password-protected` 的 readable-share E2EE public link，并在 share / metadata / saved-share 契约中暴露最小 `e2ee` 元数据。
- owner 工作台：`DriveShareLinksDrawer` 现在允许单文件 E2EE 打开分享抽屉；创建公开链接时先在浏览器本地解密 owner ciphertext，再用 share password 重新加密为 share-specific ciphertext 后上传。
- public share 页面：`public/drive/shares/[token]` 对 E2EE 文件统一改为“下载密文 → 浏览器本地解密 → 预览 / 下载”；`shared-with-me` reopen 继续复用同一路由，不新增第二套解密页面。
- 边界：batch share、folder share、collaborator readable-share E2EE 继续明确不支持；E2EE public link 变更设置仍需先撤销再重建。
- 测试：已补齐前端定向回归，覆盖分享抽屉 owner-side re-encrypt、公有分享页本地解密预览 / 下载，以及单文件分享放开但 batch share 继续阻断。

### Batch 12 范围
- 只支持 `FILE` 级 public share，不扩到 folder share descendants、batch share 或 root folder 递归解密。
- 只支持 `password-protected` public share readable E2EE：owner 在浏览器本地解密 owner ciphertext 后，再生成 share-specific ciphertext 上传，服务端继续只保存密文。
- public share 页面不再把 E2EE 文件交给服务端预览语义；必须先下载 share ciphertext，再在浏览器本地解密后预览 / 下载。
- `shared-with-me` reopen 继续复用现有 public share token 页面，因此第十二批要让 saved-share 契约携带足够的 E2EE / password 状态供路由判断。
- 第十二批不承诺 collaborator single-file decrypt、不承诺 folder descendants 解密、不承诺 share password 恢复或新的零知识元数据模型。

### Batch 12 明确不包含
- collaborator share 的公钥分发与 share-specific ciphertext
- folder share descendants / nested folders / batch share 的可读解密
- 直接暴露 owner ciphertext 给 share 读者
- 文件名 / MIME / size 的零知识化

## 明确延后
- `Drive` collaborator / folder 可读解密协议
- 外部加密邮件
- `SMTP / IMAP / Bridge`
- `Pass` 浏览器扩展 / 自动填充 / 真实 passkey ceremony / 暗网真实数据源
- `VPN / Meet / Wallet / Lumo` 默认主线深化
- 更广范围的 `components / composables` 裸字符串扫描与 release gate 升级

## 模块投资策略
| 模块 | 策略 |
|---|---|
| Mail | 继续加深，优先级最高 |
| Drive | 跟随 Mail E2EE 经验推进 |
| Calendar | 保持 GA 稳定，择机补邀请能力 |
| Docs / Sheets | 维持 Beta，不抢占当前主线 |
| Pass | 第七批升至 `BETA`，但继续保持 `Labs` 入口与受限承诺 |
| VPN / Meet / Wallet / Lumo | 维持 Preview / Labs，不进入当前主线预算 |

## 切换条件
- `Batch 12` 只有在单文件 public share E2EE 创建、public share 页面本地解密预览 / 下载、以及 `shared-with-me` reopen 复用 public route 的定向验证全部通过后，才视为关闭。

## 参考
- `docs/release/community-v1-v1.3-plan.md`
- `docs/release/community-v1-v1.2-mainline-roadmap.md`
- `docs/release/community-v1-support-boundaries.md`
