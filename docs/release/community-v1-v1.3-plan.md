# Community Edition v1.3 Planning Baseline

**版本**: `v1.3-planning-batch-12`
**日期**: `2026-04-03`
**作者**: `Codex`

## 变更记录
- `2026-04-02`：初始化 `v1.3` 规划基线，锁定首批主线为 `Mail E2EE 草稿加密 + 密钥恢复`。
- `2026-04-02`：完成 `Mail E2EE` 草稿 / 恢复 / 附件三轮收敛，冻结第三批为 `Drive E2EE foundation`。
- `2026-04-02`：完成 `Drive E2EE foundation`，冻结第四批为 `Web Push`。
- `2026-04-02`：完成第四批 `Web Push`，主线进入下一批次选择前的已验证状态。
- `2026-04-02`：冻结第五批为 `SMTP outbound adapter`，作为协议栈第一真实落点。
- `2026-04-02`：完成第五批 `SMTP outbound adapter`，并通过定向后端组合回归验证。
- `2026-04-02`：冻结第六批为 `Calendar internal invitation orchestration`，作为 Calendar 的最小邀请闭环。
- `2026-04-02`：完成第六批 `Calendar internal invitation orchestration`，attendee / invite / RSVP 主链已打通。
- `2026-04-02`：冻结第七批为 `Pass Beta readiness`，作为 Pass 从 Preview 迈向 Beta 的最小验证闭环。
- `2026-04-02`：完成第七批 `Pass Beta readiness`，Pass 当前真实能力边界、成熟度与 Labs 入口已收敛一致。
- `2026-04-03`：冻结第八批为 `a11y baseline`，优先补默认 shell 与 Pass 关键面板的可访问性基线。
- `2026-04-03`：完成第八批 `a11y baseline`，默认 shell 与 Pass 关键面板已建立第一层可访问性回归。
- `2026-04-03`：冻结第九批为 `公开 API 文档站`，优先补面向自托管采用者与集成者的浏览器内文档站入口。
- `2026-04-03`：完成第九批 `公开 API 文档站`，浏览器内 API quick page、设置页入口与运维文档已完成对齐并通过定向验证。
- `2026-04-03`：冻结第十批为 `i18n 运维化`，作为 release hardening tranche 收口现有国际化治理基线。
- `2026-04-03`：完成第十批 `i18n 运维化`，placeholder 一致性门禁、独立脚本与 CI 可见性已落地并通过定向验证。
- `2026-04-03`：冻结第十一批为 `组件 / composables i18n 裸字符串治理`，优先收口 suite / security / pass operator surfaces 的英文硬编码。
- `2026-04-03`：完成第十一批 `组件 / composables i18n 裸字符串治理`，targeted component/composable surfaces 已通过扫描、定向测试与 typecheck 验证。
- `2026-04-03`：冻结第十二批为 `Drive readable-share E2EE foundation`，优先收口单文件 public share 的 share-specific ciphertext、本地解密与 `shared-with-me` reopen。

## 规划背景
- `v1.2.0` 已正式发布，`dev/community-v1` 回到 `post-v1.2` 主线开发职责。
- `v1.2` 已经把 `Mail E2EE foundation + recipient readiness + READY 正文加密发送 / 本地解密` 落地，但邮件闭环仍存在两个高风险断点：
  1. 草稿明文保存在服务端；
  2. 用户一旦丢失私钥口令，现有账号无法恢复加密能力。
- `v1.3` 不追求产品线继续变宽，而是把“隐私优先”的主承诺继续向真实闭环推进。

## `v1.3` 核心目标
1. 把 `Mail E2EE` 从“可发送加密正文”推进到“正文 / 草稿 / 附件 / 密钥恢复一致”。
2. 把 `Drive` 从“仅明文文件管理”推进到 owner 文件主链具备最小客户端加密 foundation。
3. 继续坚持能力边界诚实，不把未交付的分享协议、外部协议栈或零知识架构写成已上线。
4. 保持 `dev/community-v1` 作为正式主线，只推进当前仓库可验证、可测试的能力。

## 批次原则
- 优先修复与当前产品承诺直接冲突的点。
- 优先选择已有基础设施可复用的实现，不引入 silent fallback、mock path 或隐式降级。
- 每个批次必须能通过本地定向验证闭环。

## `v1.3` 工作流拆分
| Stream | 主题 | 优先级 | 当前决策 | 说明 |
|---|---|---|---|---|
| A | `Mail E2EE 草稿加密` | `P0` | `Done` | 已完成，草稿保存与恢复已走本地解密路径。 |
| B | `Mail E2EE 密钥恢复` | `P0` | `Done` | 已完成，恢复包状态与恢复流程已落地。 |
| C | `Mail E2EE 附件加密` | `P0` | `Done` | 已完成，附件上传前本地加密、下载后本地解密已落地。 |
| D | `Drive E2EE foundation` | `P1` | `Done` | 第三批已完成，owner 文件主链 E2EE foundation 已闭环。 |
| E | `Web Push` | `P1` | `Done` | 第四批已完成，真实订阅和新邮件入站推送已打通。 |
| F | `SMTP outbound adapter` | `P1` | `Done` | 作为协议栈第一落点，最小真实外发能力已验证完成。 |
| G | `Calendar internal invitation orchestration` | `P1` | `Done` | 已利用现有 attendees + incoming shares 骨架补齐内部邀请闭环。 |
| H | `Pass 升级至 Beta` | `P2` | `Done` | 第七批已完成：验证真实能力并升级成熟度，不扩大功能边界。 |
| I | `a11y baseline` | `P2` | `Done` | 第八批已完成：默认 shell + Pass 关键面板的最小可访问性基线已落地。 |
| J | `公开 API 文档站` | `P3` | `Done` | 第九批已完成：浏览器内 API quick page、设置页入口与运维文档对齐已验证通过。 |
| K | `i18n 运维化` | `P3` | `Done` | 第十批已完成：placeholder 一致性门禁、独立脚本与 CI 独立可见面。 |
| L | `组件 / composables i18n 裸字符串治理` | `P3` | `Done` | 第十一批已完成：suite / security / organizations operator surfaces 裸字符串扫描并修复当前英文硬编码。 |
| M | `Drive readable-share E2EE foundation` | `P3` | `Done` | 第十二批已完成并通过定向验证：单文件 public share password-protected share ciphertext、本地解密与 `shared-with-me` reopen 已收口。 |
| N | `剩余 i18n 运维化增强` | `P3` | `Backlog` | 更广覆盖的 components / composables 裸字符串扫描与更强发布门禁仍未落地。 |
| O | `Drive collaborator / folder readable-share E2EE` | `P3` | `Backlog` | collaborator 公钥分发、folder descendants 与批量分享仍需更重的协议 tranche。 |

## 已完成前两批范围
### 1. Mail E2EE 草稿加密
- 前端保存草稿时支持本地加密正文。
- 后端草稿保存支持接收 `Mail E2EE` 密文载荷并持久化元数据。
- 加密草稿重新打开时，不直接回填密文；必须经浏览器内显式解密后才进入编辑态。
- 自动保存与手动保存行为一致，不允许自动保存绕过加密路径。

### 2. Mail E2EE 密钥恢复
- 为已启用的 Mail E2EE key profile 增加恢复包元数据和显式开关。
- 恢复包必须是额外口令保护的加密私钥包，不得把未加密私钥写入服务端。
- 恢复流程必须显式、可关闭，并保留用户可见状态。
- 恢复失败必须抛出真实错误，不做静默兜底。

### 3. Mail E2EE 附件加密
- 附件上传前在浏览器本地加密。
- 后端附件存储仅保存密文 blob 与最小 E2EE 元数据。
- 附件下载后在浏览器本地解密，不把明文回传给服务端。

## 已批准第三批范围
### 4. Drive E2EE foundation
- owner `My Files` 文件上传前支持本地加密。
- owner 文件版本上传与恢复必须保留 E2EE 元数据，不允许版本链路退回明文语义。
- owner 文件下载继续返回密文 blob，由浏览器侧负责本地解密。
- owner 文件预览改为“下载密文后本地解密再渲染”；服务端预览对 E2EE 文件显式报错。
- `DriveItem` / `DriveFileVersion` 对前端暴露最小 E2EE 元数据，用于 UI 分流。
- 对 E2EE 文件先禁用现有 public share / collaborator share 可读入口，避免密文被误当明文能力宣传。

## 已批准第四批范围
### 5. Web Push
- 新增 owner 级 `Web Push subscription` 数据模型，显式保存 endpoint、key material、user agent 和最近成功下发时间。
- 新增注册 / 删除 / 查询订阅 API，要求依赖真实浏览器 `PushManager` 返回值，不做 mock payload 兼容路径。
- 使用显式 `VAPID` 配置进行真实下发；缺配置时返回真实错误，不做 silent fallback。
- 新邮件入站复制成功后触发最小 `Mail inbox` 推送事件，标题与正文摘要只暴露当前系统已允许的最小展示信息。
- `sw.js` 接收推送并显示通知；`notificationclick` 需要优先聚焦现有窗口，否则打开 inbox 或邮件详情页。
- 设置页显示订阅状态，并支持授权后订阅、退订和失败显式反馈。

## 已批准第五批范围
### 6. SMTP outbound adapter
- 新增独立的 outbound delivery adapter，而不是把 `SMTP` 细节散落进现有 controller。
- 对外部收件人建立最小真实外发能力：当前 `Mail` 主路径允许直接投递到外部邮箱，不再统一报 `Unable to deliver mail`。
- 复用现有 `OUTBOX / SCHEDULED` 语义：到期派发时对内部目标继续 inbox copy，对外部目标走 `SMTP outbound gateway`。
- 缺少 `SMTP` 配置时，对外部收件人发送必须返回真实错误，不做 silent fallback 或 fake success。
- 第五批只承诺正文 / 标题级外发链路，不把 `SMTP inbound`、`IMAP`、`Bridge`、域名信誉校验、外部 E2EE 或完整 MIME 兼容一次性塞进当前批次。

## 已批准第七批范围
### 8. Pass Beta readiness
- 后端新增 `PassReleaseBlockingIntegrationTest`，收敛当前已经真实实现的 `Pass` 主路径，而不是继续扩功能面。
- 个人工作台至少覆盖：item CRUD / favorite、公开 secure link 可读、mailbox create / verify / default、alias create / reroute / relay 的真实边界。
- 共享工作台至少覆盖：shared vault、成员加入、shared item、incoming shared item detail、org secure links dashboard 等已交付能力。
- 前端新增 `pass-smoke.spec.ts`，覆盖 `/pass`、`/pass-monitor` 与公开 secure link 页面最小关键路径，并确认错误展示与工作台切换不回退。
- 模块成熟度调整为 `PASS = BETA`，但 `surface` 继续保持 `LABS`，不进入默认导航与首页候选。

## 已批准第八批范围
### 9. a11y baseline
- 默认 shell 增加显式 `skip to content` 链接、导航与主内容 landmark、搜索输入与安全入口标签，优先覆盖登录后的主工作台骨架。
- `PassSidebarPanel` 与公开 `Pass secure link` 页面补选择态 / 区域标签 / 动作语义，避免键盘与读屏用户落入“看得到但说不清”的状态。
- 所有新增语义必须走现有三语 locale catalog，不能引入只在英文存在的 key。
- 第八批测试只做当前交付面的定向回归：新的 a11y spec、相关 `pass-smoke` 回归与 `pnpm typecheck`。
- 第八批不引入新的第三方 a11y 扫描依赖，不做全仓 Element Plus 组件包装，也不声明完整 `WCAG` 合规。

## 已批准第九批范围
### 10. 公开 API 文档站
- 新增 `frontend/public/self-hosted/api.html`，作为当前主线 `Swagger UI`、`OpenAPI JSON`、install / runbook 的统一浏览器入口。
- API quick page 必须优先消费设置页传入的 `apiBase` 查询参数；如果缺失，不允许 silent fallback 到猜测 origin，而要显式提示用户输入后端地址。
- 设置页 `Adoption readiness` 面板保留 `Swagger UI` 与 `OpenAPI JSON` 直链，同时新增 API quick page 链接，便于自托管用户在当前部署 base 上浏览文档。
- `frontend/public/self-hosted/install.html`、`frontend/public/self-hosted/runbook.html`、`docs/ops/install.md` 与 `docs/ops/runbook.md` 对齐当前 `v1.3` 主线真实边界与入口。
- 第九批只交付文档站与入口整理；不新增 SDK、公共开发者门户、认证协议或新的后端 API surface。

## 已批准第十批范围
### 11. i18n 运维化
- `frontend/utils/i18n-governance.ts` 在 key 对齐之外，新增 placeholder 集合一致性检查。
- `frontend/package.json` 提供独立 `i18n:test`、`i18n:catalog`、`i18n:coverage`、`i18n:check` 脚本，避免 i18n 门禁只能嵌在大一统命令里。
- `.github/workflows/ci.yml` 增加独立 `Frontend i18n gates` step，并把模块数、gap 数、placeholder mismatch 数、page coverage 写入 summary。
- `scripts/validate-local.sh` 继续保留 i18n 分段日志，但改为调用新的前端脚本入口。
- 第十批不做 Crowdin、翻译平台集成，不做 components / composables 裸字符串扫描，不引入新的 locale。

## 已批准第十一批范围
### 12. 组件 / composables i18n 裸字符串治理
- 在现有 `page coverage + locale catalog` 之外，新增对关键 `components / composables` 的首批裸字符串扫描。
- 扫描目标限定为当前主线真实高频 operator surfaces：`SuiteReadinessSecurityPanel`、`SuiteCommandSearchPanel`、`SuiteGovernancePanel`、`SuiteRemediationPanel`、`SecurityAliasQuickCreate`、`OrganizationsPolicyPanel`、`useSuiteOperationsWorkspace`。
- 当前已识别的英文硬编码必须改为走三语 locale catalog，不允许保留在模板文本、placeholder、按钮文案或 `ElMessage` 消息里。
- `frontend/utils/suite-operations.ts` 中返回给这些面板的概览标签、阶段标签、SLA 文案与 item type 文案也必须切到 locale key。
- 第十一批的门禁只承诺 targeted scan，不引入全仓 AST lint，也不把 Preview / Labs 大面积纳入强制失败范围。

## 已批准第十二批范围
### 13. Drive readable-share E2EE foundation
- 只支持 owner 对 `FILE` 级 public share 创建 share-specific ciphertext；owner ciphertext 不允许直接暴露给 public reader。
- 只支持 `password-protected` public share readable E2EE：share 密码既是访问门槛，也是 share-specific ciphertext 的对称解密口令。
- public share metadata / saved-share 契约需暴露最小 E2EE 元数据，前端据此切换到“下载密文 → 浏览器本地解密 → 预览 / 下载”的路径。
- `shared-with-me` reopen 继续复用 `public/drive/shares/[token]` 页面，不新建第二套 share decrypt UI。
- 第十二批不扩展 collaborator share、公钥分发、folder descendants、batch share，也不承诺文件名 / MIME / size 的零知识化。

## 明确不进入第十二批
1. `Drive` collaborator workspace 的可读解密协议
2. folder share descendants / nested folders / batch share 的可读解密
3. 文件名 / MIME / size 的零知识化
4. 外部收件人 password-protected encrypted mail
5. `SMTP inbound / IMAP / Bridge`
6. `Pass` 浏览器扩展与自动填充
7. 真实 `WebAuthn / passkey ceremony`
8. 暗网真实数据源或泄露情报接入
9. `VPN / Meet / Wallet / Lumo` 的默认代码库扩展
10. 全仓 `components / composables` 通用 AST 级裸字符串 lint

## 产品线约束
- `VPN / Meet / Wallet / Lumo` 保持 `Preview / Labs`，当前不投入默认主线开发预算。
- `Pass / SimpleLogin / Authenticator / Standard Notes` 保持独立评估，不挤占 Mail 核心闭环。
- `Community Edition` 继续保持 `Hosted-only billing` 口径，不承诺支付闭环。

## 第四批完成判定
- owner 账号可以在已授权浏览器上完成 `Web Push` 订阅注册、查看状态与显式退订。
- 服务端在新邮件入站复制成功后触发真实 `Web Push` 发送尝试，并能记录最近一次成功或失败状态。
- `sw.js` 可以处理 `push` 与 `notificationclick`，通知点击后能聚焦或打开正确路由。
- 定向前后端测试在本地通过，并满足 `timeout 60s` 约束。
- 当前仓库已满足以上判定，第四批可以视为已验证完成。

## 第五批完成判定
- 外部收件人地址不再被当前路由系统一律判定为不可达。
- 后端对外部目标能够通过显式 `SMTP` 配置发起真实 outbound 发送尝试，并保留失败显式错误。
- 当前 `Mail` 主路径对内部目标语义不回退，内部 inbox copy 和第四批 `Web Push` 行为不被破坏。
- 定向后端集成测试在本地通过，并满足 `timeout 60s` 约束。
- 当前仓库已满足以上判定，第五批可以视为已验证完成。

## 已批准第六批范围
### 7. Calendar internal invitation orchestration
- 创建或更新事件时，attendee 邮箱命中本地 `UserAccount` 的场景，自动同步为内部 invite/share，不再要求 owner 再手工走一次 share 面板。
- 继续复用现有 `/api/v1/calendar/shares/incoming` 与 `/api/v1/calendar/shares/{shareId}/response` 作为内部邀请与 RSVP 主链，不新起一套 invite token 模型。
- owner 读取事件详情时，attendee `responseStatus` 必须能反映 invite 的当前响应状态，而不是永远停留在 `NEEDS_ACTION`。
- 事件更新时，attendee 列表与内部 invite/share 要保持同步，至少保证新增内部 attendee 能获得 invite，移除的 auto-managed 内部 attendee 不再残留旧 invite。
- 前端继续在现有 `calendar.vue` 工作台上落地，不新增独立邀请页面；现有 incoming share / attendee detail UI 按 invitation 语义做最小增强。

## 明确不进入第六批
1. 外部邮箱邀请邮件
2. invite token / RSVP link
3. Calendar → Mail notification 发件链路
4. Meet 联动
5. 高级重复规则或外部订阅增强

## 第七批完成判定
- `PassReleaseBlockingIntegrationTest` 覆盖并通过个人工作台、共享工作台、mailbox / alias、secure link public read 的最小真实主路径。
- `pass-smoke.spec.ts` 通过，并且现有 `pass-business.spec.ts`、`pass-monitor.spec.ts`、`community-navigation.spec.ts`、`i18n.spec.ts` 不回退。
- `frontend/constants/module-maturity.ts`、`docs/open-source/module-maturity-matrix.md` 与相关边界文档对 `PASS` 的成熟度 / 入口 / 非目标保持一致。
- `PASS` 升级为 `BETA` 后仍停留在 `Labs` 暴露面，不进入默认导航，不把未交付能力写成已上线。

## 第八批完成判定
- 默认 shell 存在键盘可达的 `skip link`、可识别的导航 landmark 与主内容锚点。
- `PassSidebarPanel` 能向辅助技术暴露当前选中项与可操作对象的描述信息。
- 公开 `Pass secure link` 页面能向辅助技术暴露页面主区域、摘要区和敏感字段动作的明确语义。
- 新增前端 a11y 定向测试通过，并且现有 `pass-smoke.spec.ts` 与 `pnpm typecheck` 不回退。

## 第九批完成判定
- 设置页 `Adoption readiness` 面板能暴露当前 `apiBase` 对应的 API quick page、`Swagger UI` 与 `OpenAPI JSON` 入口。
- `frontend/public/self-hosted/api.html` 在缺少 `apiBase` 时显式提示，在提供 `apiBase` 时能正确生成 `Swagger UI`、`OpenAPI JSON` 与 health check 链接。
- install / runbook quick page 与仓库运维文档不再停留在 `v1.2` 边界描述，并且都能指向新的 API quick page。
- 新增定向前端测试通过，并且 `pnpm typecheck` 不回退。

## 第十批完成判定
- `frontend/utils/i18n-governance.ts` 能对 locale key 缺失和 placeholder 不一致同时报错。
- `frontend/package.json` 暴露 `i18n:test`、`i18n:catalog`、`i18n:coverage`、`i18n:check` 脚本，且本地可直接执行。
- `.github/workflows/ci.yml` 中 `Frontend i18n gates` step 独立运行，并把关键指标写入 summary。
- 定向 `i18n` 测试、报告脚本与 `pnpm typecheck` 不回退。

## 第十一批完成判定
- targeted `components / composables` i18n 裸字符串扫描可以对当前 operator surfaces 报出显式违规文件。
- `SuiteReadinessSecurityPanel`、`SuiteCommandSearchPanel`、`SuiteGovernancePanel`、`SuiteRemediationPanel`、`SecurityAliasQuickCreate`、`OrganizationsPolicyPanel`、`useSuiteOperationsWorkspace` 中当前识别出的英文硬编码全部切到 locale key。
- `frontend/utils/suite-operations.ts` 返回到这些面板的用户可见标签不再内置英文。
- `pnpm i18n:check` 与 `pnpm typecheck` 通过，且相关定向 i18n 测试不回退。

## 第十二批完成判定
- owner 可以为 `FILE` 级 E2EE public share 上传 share-specific ciphertext，且后端只保存密文与最小 E2EE 元数据。
- `public/drive/shares/[token]` 对 E2EE 文件走“下载密文 → 本地解密 → 预览 / 下载”路径，而不是继续依赖服务端预览语义。
- `shared-with-me` reopen 对同一 token 复用 public share 页面，不新增第二套解密堆栈。
- 定向后端 / 前端测试通过，并满足既有 `timeout 60s` 与 `pnpm typecheck` 约束。

当前仓库已满足以上判定，第十二批可视为已验证完成，`v1.3` 当前主线计划至此收口。

## 参考文档
- `docs/release/community-v1-v1.2-plan.md`
- `docs/release/community-v1-v1.2-mainline-roadmap.md`
- `docs/release/community-v1-v1.2-capability-boundaries.md`
- `docs/open-source/module-maturity-matrix.md`
