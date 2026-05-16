---
name: v2.1.2 迁移进度核对报告
date: 2026-05-16
spec: docs/v212-migration-spec.md (v1.2)
scope: §27 验收清单 13 组、§14 差距 29 项 / §6.4 框架复用
auditor: Claude (本次会话)
verdict: 部分完成 — spec 未全部闭环，建议进入收尾阶段
---

# v2.1.2 迁移进度核对报告

## 摘要（一段话）

`frontend-admin/` 已经按 v2.1.2 spec **进入工程实施期且推进很深**：23 个业务 view 模块全部建立、F-A 7 个完全缺失模块和 F-B 9 项部分缺失功能里只有 1 项（Business Overview 独立视图）未交付；后端 B-A 5 个新增模块全部到位，B-B 8 项扩展全部有实现 + 数据迁移 + 测试；§6.4 强制复用约束的 3 个 CI 脚本与禁库扫描已落地；EntitlementGate / WS 网关 / 审计 13 事件 / 数据 seed / Prometheus 全通。**主要遗留**集中在「前端测试覆盖薄弱（仅 2 个测试文件）」「Business Overview 独立入口未接」「五态组件未抽象成统一组件库」三件事，其余均已闭环或属于命名差异（如迁移文件用 V21–V32 而非 V2_1_2__）。

**结论：spec 未 100% 完成，但 ≥ 90% 已闭环，建议出收尾任务清单进入 release-gate，不需要再写新设计。**

---

## 1. 数据采集口径

| 维度 | 数据 |
|---|---|
| spec 总章节 | 29 节（§0–§28），3113 行 |
| §27 验收清单 | 13 组（27.1–27.13） |
| §14 拆分需求 | F-A 7 + F-B 9 + B-A 5 + B-B 8 = 29 项 |
| 前端 view 模块（Soybean） | 23 个目录，36 个 .vue 文件 |
| 后端 controller 改动 | 379 项未提交修改 + 多处新增 |
| 后端 v2.1.2 期数据库迁移 | V21–V32（10 个文件，含 RRULE / lexorank / WebPush / 社区 / 搜索 / 邮件外部账号 / CRDT 快照 / 审计 metadata / feature_flags） |
| 后端 v2.1.2 期测试 | 138 个 *.java 测试文件（>=8 个 V212 命名集成测试） |
| 前端测试 | 2 个（覆盖率严重不足） |

---

## 2. §27 验收清单核对（13 组）

> 状态图例：✅ 完成 / 🟡 部分 / ⛔ 未做 / 🔁 命名差异但等价

| 组别 | 验收项 | 状态 | 证据 / 缺失 |
|---|---|---|---|
| 27.1 | 13 个业务模块页面 | ✅ | views/ 下 mail/calendar/drive/docs/sheets/pass/wallet/vpn/meet/contacts/community/search/notes/collaboration/admin/settings/share/notifications/command-center/integrations/security 全部有 index.vue（最少 84 行，wallet/meet 400+ 行实质实现） |
| 27.1 | 登录 / 403 / 404 / 500 | ✅ | views/_builtin/ 下 4 类异常页 + login(142 行) |
| 27.1 | 多语言切换 | ✅ | locales/langs/zh-cn.ts + en-us.ts 全模块覆盖 |
| 27.1 | 主题 / 标签页 / 响应式 | ✅ | 沿用 Soybean 内置 store/theme + store/tab |
| 27.2 | F-A 7 个模块 | ✅ | wallet(442) / vpn(408) / meet(462) / contacts(213) / integrations/simplelogin(98) / notes(212) / security/authenticator(318) 全部 ≥ 100 行实质代码 |
| 27.3 | F-B 9 项 | 🟡 | 8/9 完成。**Suite 工作台真数据**已接（home/index.vue 调 workspace api）；**Business Overview 独立视图未建**（后端 OrgBusinessController 已就绪，前端无 business-overview 路由/视图）；其余邮件规则/外部账号/Drive 版本/E2EE 分享/域名/WebPush/Admin 计费均接入 |
| 27.4 | B-A 5 个后端模块 | ✅ | CommunityController / SearchController / CommandCenterController（V21OpsController 内）/ DomainController / WebPushController + 对应 service/mapper/migration 齐全 |
| 27.5 | B-B 8 项 | ✅ | CalDAV → CalendarSubscription 全套(7 文件) / RRULE → CalendarRecurrenceService(389 行) / IMAP/SMTP → MailExternalAccountController / CRDT → V26 迁移 + docs-collab-crdt hook + 集成测试 / 公式 → SheetsFormulaController / lexorank → V22 board_positions 迁移 + V21CollaborationWriteService / WS 推送 → NotificationWebSocketConfig / 异常检测 → SecurityEventController + V25 迁移 |
| 27.6 | EntitlementGate + 路由 meta | ✅ | components/access/EntitlementGate.vue(99 行) + router/routes/access-meta.ts 声明 premiumOnly/featureFlag/requiresEntitlement |
| 27.7 | WS 网关后端 + 前端 | ✅ | 后端 CollabWebSocketConfig + NotificationWebSocketConfig + WsAuth 鉴权；前端 hooks/business/notification-realtime.ts + docs-collab-crdt.ts（spec §20 未要求"独立 ws.ts"，分散在 hook 内可接受） |
| 27.8 | 数据迁移 | 🔁 | spec §21 建议命名 V2_1_2__01…，实际采用 V21–V32 单调编号（10 个 v2.1.2 期迁移），文件内 ROLLBACK 注释规范，等价闭环 |
| 27.9 | ErrorCode 段位 | 🟡 | ErrorCode.java 共 85 个码（10001–90000 范围），但**未按 spec §22 段位明确分块注释**；GlobalExceptionHandler 已统一返回结构 |
| 27.9 | 五态组件 | 🟡 | 未抽出独立 EmptyState/ErrorState/LoadingState/SuccessState 组件；views 内直接使用 Naive UI 的 NEmpty/NSpin/NAlert/NSkeleton（功能等价但**未达成 spec §22.2 "五态规范"统一封装**） |
| 27.10 | i18n 全模块 | ✅ | zh-cn / en-us 两份完整翻译，覆盖 mail/calendar/drive/docs/sheets/pass/wallet/vpn/meet/community/contacts/notes/settings/admin/security 等 |
| 27.10 | 审计 13 事件 | ✅ | AuditEventRegistry 注册 13 个 v2.1.2 事件类型（wallet.tx.*、meet.host.transfer、domain.add/delete、totp.*、community.*、auth.login.high_risk、billing.subscription.action、webpush.subscription.delete） |
| 27.10 | 业务指标命名 | ✅ | WebSocketMetricsService 暴露 ws_active_connections；application.yml 暴露 prometheus 端点；pom.xml 引 micrometer-registry-prometheus |
| 27.11 | 6 个 seed 文件 | ✅ | data-seed/ 下 community.sql / search-index.sql / webpush.sql / domain.sql / meet.sql / wallet.sql 全部存在 |
| 27.12 | Prometheus / Grafana | ✅ | application.yml: management.endpoints.web.exposure.include=health,info,metrics,prometheus；ops/grafana/ws-gateway-dashboard.json 已上线；ops/ws-gateway-load-test.mjs 压测脚本就绪 |
| 27.12 | 测试矩阵 | 🟡 | **后端 138 个测试文件覆盖充分（含 8+ 个 V212 命名集成测试）；前端仅 2 个测试文件，覆盖率不达 §26.3 要求** |
| 27.12 | 安全清单 | ✅ | SECURITY.md + docs/security/ + docs/security-audit.md 三处 |
| 27.13 | check-style-discipline.mjs | ✅ | scripts/check-style-discipline.mjs(3457 字节) |
| 27.13 | gen-api.mjs | ✅ | scripts/gen-api.mjs(762 字节) + service/api/__generated__/openapi.d.ts 已生成 |
| 27.13 | check-bundle-budget.mjs | ✅ | scripts/check-bundle-budget.mjs(3018 字节) |
| 27.13 | SCSS 文件位置 | ✅ | src/styles/scss/ 外无新增 SCSS |
| 27.13 | 禁用依赖白名单 | ✅ | package.json 无 element-plus / ant-design-vue / tailwindcss / moment / sortablejs / chart.js / d3 / mapbox-gl；唯一保留的 tailwind-merge 是工具函数（合并 class 字符串）非样式库 |

### 13 组合并状态

| 组 | 整体状态 |
|---|---|
| 27.1 原始迁移项 | ✅ 完成 |
| 27.2 F-A 7 模块 | ✅ 完成 |
| 27.3 F-B 9 项 | 🟡 部分（缺 Business Overview 视图） |
| 27.4 B-A 5 模块 | ✅ 完成 |
| 27.5 B-B 8 项 | ✅ 完成 |
| 27.6 共享组件门控 | ✅ 完成 |
| 27.7 WS 网关 | ✅ 完成 |
| 27.8 数据迁移 | 🔁 等价完成（命名差异） |
| 27.9 错误码与状态 | 🟡 部分（错误码未分段注释 + 五态未抽组件） |
| 27.10 i18n / 审计 / 可观测性 | ✅ 完成 |
| 27.11 测试数据 seed | ✅ 完成 |
| 27.12 跨切面 | 🟡 部分（前端测试覆盖率严重不足） |
| 27.13 框架复用 | ✅ 完成 |

**13 组：完成 9 / 等价 1 / 部分 3 / 未做 0**
**完成率：(9 + 1 + 3*0.5)/13 ≈ 88%**

---

## 3. §14 差距 29 项核对（按 F-A / F-B / B-A / B-B 矩阵）

### 3.1 F-A：前端完全缺失 7 项

| # | 模块 | 状态 | 说明 |
|---|---|---|---|
| 15.1 | 钱包 Wallet | ✅ | views/wallet/index.vue 442 行；store/modules/wallet 单独建立；service/api/wallet.ts |
| 15.2 | VPN | ✅ | views/vpn/index.vue 408 行；service/api/vpn.ts |
| 15.3 | 会议 Meet（4 子项） | ✅ | views/meet/index.vue 462 行；service/api/meet.ts；4 子项（房间生命周期 / 媒体 / 访客审批 / 信令）均已闭环 |
| 15.4 | 联系人 Contacts | ✅ | views/contacts/index.vue 213 行 |
| 15.5 | SimpleLogin | ✅ | views/integrations/simplelogin/index.vue 98 行 |
| 15.6 | Standard Notes | ✅ | views/notes/index.vue 212 行 |
| 15.7 | TOTP / Authenticator | ✅ | views/security/authenticator(318 行) + AuthenticatorController 后端 |

**F-A：7/7 完成 ✅**

### 3.2 F-B：前端部分缺失 9 项

| # | 功能 | 状态 | 说明 |
|---|---|---|---|
| 16.1 | Suite 工作台真数据 | ✅ | home/index.vue 调 readWorkspaceSummary / listWorkspaceActivity / listWorkspaceTasks |
| 16.2 | Business Overview 真数据 | ⛔ | **缺失独立视图**：views/business-overview/ 不存在；后端 OrgBusinessController 已 ready 等接 |
| 16.3 | 邮件规则编辑 UI | ✅ | views/mail/rules/MailRulesPanel.vue |
| 16.4 | 邮件标签 / 文件夹拖拽 | ✅ | mail/index.vue 内集成 |
| 16.5 | Drive 版本历史 UI | ✅ | drive/index.vue 调 listDriveFileVersions / restoreDriveFileVersion |
| 16.6 | Drive E2EE 分享 | ✅ | views/share/index.vue 3704 字节 |
| 16.7 | 设置-自定义域名 | ✅ | settings/index.vue 调 createAdminDomain / listAdminDomains / listAdminDomainDnsRecords |
| 16.8 | 设置-Web Push | ✅ | settings/index.vue 调 listWebPushSubscriptions / deleteWebPushSubscription |
| 16.9 | Admin 计费 / 订阅 | ✅ | admin/index.vue 调 SuiteBilling 全套接口 |

**F-B：8/9 完成（缺 Business Overview 视图）**

### 3.3 B-A：后端完全缺失 5 项

| # | 模块 | 状态 |
|---|---|---|
| 17.1 | 社区 Community（6 子项） | ✅ CommunityController + V27 迁移 + 测试 + seed |
| 17.2 | 全局搜索（4 子项） | ✅ SearchController + V28 迁移 + seed |
| 17.3 | 命令面板契约 | ✅ V21OpsController.command-center + V21 迁移（command_panel_preferences） |
| 17.4 | 自定义域名 Domain | ✅ DomainController + service/seed |
| 17.5 | Web Push 订阅 API | ✅ WebPushController + V8/V23 迁移 + seed |

**B-A：5/5 完成 ✅**

### 3.4 B-B：后端部分缺失 8 项

| # | 功能 | 状态 | 关键证据 |
|---|---|---|---|
| 18.1 | 日历订阅（CalDAV/ICS） | ✅ | CalendarSubscription* 7 文件 + V29 迁移 |
| 18.2 | RRULE 重复事件 | ✅ | CalendarRecurrenceService(389 行) + V24 迁移 |
| 18.3 | IMAP/SMTP 外部账号 | ✅ | MailExternalAccountController + V30 迁移 |
| 18.4 | 文档实时协同 CRDT | ✅ | V26 collab_crdt_snapshot_update 迁移 + DocsCollabCrdtV212ContractIntegrationTest + 前端 docs-collab-crdt hook（按 spec 18.4.1 + 18.4.2 交付，18.4.3 已声明滚动 v2.1.3） |
| 18.5 | 表格服务端公式 | ✅ | SheetsFormulaController + SheetsFormulaCellResultVo |
| 18.6 | 看板拖拽排序持久化 | ✅ | V22 board_positions 迁移 + V21CollaborationWriteService 内 lexorank 实现 |
| 18.7 | 通知 WebSocket 推送 | ✅ | NotificationWebSocketConfig + NotificationWebSocketHandshakeInterceptor + 前端 notification-realtime hook |
| 18.8 | 登录异常检测 | ✅ | SecurityEventController + V25 login_security_events 迁移 + AuditEventRegistry 注册 auth.login.high_risk |

**B-B：8/8 完成 ✅**

### 3.5 §14 总分

| 类别 | 应交付 | 已交付 | 缺口 |
|---|---|---|---|
| F-A | 7 | 7 | 0 |
| F-B | 9 | 8 | 1（Business Overview 视图） |
| B-A | 5 | 5 | 0 |
| B-B | 8 | 8 | 0 |
| **合计** | **29** | **28** | **1** |

**§14 差距闭合率：96.6%（28/29）**

---

## 4. §6.4 Soybean 框架复用强制约束 自检

| 检查项 | 结果 | 证据 |
|---|---|---|
| 6.4.1 必须复用 11 个布局组件 | ✅ | base-layout / blank-layout / 10 个 layouts/modules/* 均沿用未自研 |
| 6.4.1 必须复用 16 个通用组件 | ✅ | components/common/ 13 + components/custom/ 7 + components/advanced/ 2 全部存在 |
| 6.4.1 必须复用 5 个 store | ✅ | store/modules/{app,auth,route,tab,theme} 全部使用，新增 wallet 与 org 两个业务 store |
| 6.4.2 禁止 SCSS 新增 | ✅ | SCSS 仅在 src/styles/scss/ 原文件 |
| 6.4.2 禁止自研 CSS 变量 | ✅ | 全仓搜索无 `--mm-*` / `--v211-*` |
| 6.4.2 禁止第二组件库 | ✅ | package.json dependencies 仅 naive-ui，无 element-plus/antd-vue |
| 6.4.4 白名单依赖 | ✅ | vue-draggable-plus 已在 lock；monaco-editor / Tiptap 未引入（按需懒加载，可后续添） |
| 6.4.4 默认拒绝列表 | ✅ | 无 mapbox-gl / chart.js / d3 / sortablejs / moment / tailwindcss |
| 6.4.5 CI 三脚本 | ✅ | check-style-discipline.mjs / gen-api.mjs / check-bundle-budget.mjs 三件都在 scripts/ |
| 6.4.5 OpenAPI 类型同步 | ✅ | service/api/__generated__/openapi.d.ts 存在 |

**§6.4 通过率：10/10 ✅**

---

## 5. 主要遗留问题（需要在收尾阶段处理）

按风险与工作量排序：

### 5.1 高优先级（建议本期闭环）

1. **Business Overview 独立视图缺失（F-B 2）**
   - 现状：后端 `OrgBusinessController` 已就绪且接口完备，但前端 `views/business-overview/` 不存在，路由也未挂载
   - 影响：spec §16.2 验收项不达标；机构客户进 Suite 后看不到组织级总览
   - 建议工作量：1.5 PD（参照 home/index.vue 改写）

2. **前端测试覆盖严重不足**
   - 现状：`tests/{unit,component}` 仅 2 个测试文件；后端 138 个测试文件相比之下差距悬殊
   - 影响：spec §26.3 测试矩阵不达标；EntitlementGate / 路由守卫 / WS 重连 / OpenAPI 类型同步等关键路径无回归保护
   - 建议工作量：3–5 PD（先补 EntitlementGate 4 种 fallback、wallet/meet 主流程、登录路由守卫共约 20 个 unit/component test）

3. **五态组件未统一封装（§22.2）**
   - 现状：views 内直接散用 NEmpty/NSpin/NAlert/NSkeleton，每个页面写法不一致
   - 影响：长期维护时文案 / 间距 / 主题色容易飘
   - 建议工作量：1 PD（components/feedback/{EmptyState,ErrorState,LoadingState,SuccessState}.vue 4 个壳组件 + 替换 ~10 处用法）

### 5.2 中优先级（可滚动 v2.1.3）

4. **错误码未分段注释（§22.1）**
   - 现状：`ErrorCode.java` 85 个码无分段注释，难以快速定位 v2.1.2 新增段位
   - 建议：在 ErrorCode.java 顶部加段位注释，无代码改动
   - 工作量：0.5 PD

5. **数据迁移文件命名不符 spec 建议**
   - 现状：用 V21–V32 而非 V2_1_2__01..；功能等价但 spec §21 命名规约未严格执行
   - 建议：保持现状，更新 spec §21 接受 V{N} 单调编号即可（命名只是建议非硬约束）
   - 工作量：0 PD（改 spec 即可）

### 5.3 低优先级（不阻塞发布）

6. **前端 WS 服务未独立成 ws.ts**
   - 现状：分散在 hooks/business/{notification-realtime,docs-collab-crdt}.ts
   - 评估：spec §20 未硬性要求独立模块，分散在 hook 内符合 Vue Composition 模式，**接受现状**

7. **`tailwind-merge` 引入**
   - 现状：package.json dependencies 含 `tailwind-merge`（合并 class string 工具）
   - 评估：非样式库本体，是 UnoCSS class 合并辅助，**接受现状**

---

## 6. 工程实施期质量评估

| 维度 | 评分 | 备注 |
|---|---|---|
| spec 遵循度 | A- | 29 项差距闭合 28，13 组验收过 9 完整 + 1 等价 + 3 部分 |
| Soybean 框架复用纪律 | A | §6.4 强制约束 10/10 全过；无禁库引入；无新 SCSS |
| 后端工程深度 | A | 10 个 v2.1.2 期迁移 + 138 测试 + Prometheus + 审计 13 事件 + WS 网关全栈 |
| 前端工程深度 | B+ | 业务模块完整但**测试薄弱**（2/138 vs 后端） |
| 数据契约闭环 | A | OpenAPI 类型生成 + 6 seed + Flyway 迁移规范 |
| 可观测性 | A- | Prometheus 端点 + Grafana WS 面板 + 审计事件结构化；缺 trace 链全面接入 |
| 安全合规 | A | EntitlementGate + 注解切面 + 安全清单 3 份文档 |

**综合评分：A-（90 分）**

---

## 7. 验收建议

### 7.1 准 release 条件（建议这两件做完才进 v2.1.2 release-gate）
- [ ] 补 Business Overview 视图（5.1.1）
- [ ] 前端测试覆盖率达 spec §26.3 最低线（≥ 20 个测试文件，覆盖关键守卫与 EntitlementGate）

### 7.2 release 后跟进（v2.1.3 滚入）
- [ ] 五态组件封装统一（5.1.3）
- [ ] ErrorCode 段位注释化（5.2.4）
- [ ] spec §21 迁移命名规约修订（5.2.5）
- [ ] 18.4.3 Sheets/看板 CRDT 接入（spec 内已声明 v2.1.3）

### 7.3 spec 文档处理
- spec v1.2 现状准确反映需求；本期不需要再迭代 spec
- 建议在 §0 状态栏追加 v1.3 摘要说明"§21 迁移文件命名采用 V{N} 单调编号"，工作量 0.1 PD

---

## 8. v2.1.3 收尾完成（2026-05-16）

- [x] T-1 Business Overview 独立视图：路由、API 封装、团队空间表格与抽屉闭环。
- [x] T-2 前端测试覆盖深化：v212 contract/unit/component/e2e 入口接入 release-gate。
- [x] T-3 五态组件统一封装：缺 org 错误态改为 ErrorState，剩余信息态 NAlert 加注释锚定。
- [x] T-4 ErrorCode 段位注释化：错误码分段与契约测试已纳入后端回归。
- [x] T-5 spec §21 迁移命名规约修订：迁移命名检查脚本进入 release-gate step 12。
- [x] T-6 Docker 化测试基线：`scripts/run-tests-docker.sh` 覆盖 backend/contract/unit/e2e。
- [x] T-7 release-gate 自动化校验脚本：12 步门禁接入本地与 CI。
- [x] U-1 五态替换留尾：admin 缺 org 使用 ErrorState，admin/calendar/share 剩余 4 个 NAlert 为信息态。
- [x] U-2 oxfmt 漂移收敛：frontend-v2 增加 oxfmt，release-gate step 4 恢复硬阻断。
- [x] U-3 e2e 实测落地：6 个 v212 e2e spec 去除 skip，并接入 Docker mysql/redis 与宿主机真实 backend/frontend。
- [x] U-4 CI 接 release-gate：`docker-test-baseline` 跑 e2e，`release-gate` job 依赖 frontend/backend/docker。
- [x] U-5 文档收尾：progress、CHANGELOG、v1.0/v1.1 状态栏完成。
- [x] U-6 v2.1.4 归宿：`docs/v214-roadmap.md` 承接三项不在范围工作。

## 9. v2.1.2 上线清理完成（2026-05-16）

- [x] `frontend-admin/` 承接 v2.1.x admin / 工作台前端，旧开源项目目录名已从活动脚本、CI、测试和文档引用中移除。
- [x] 顶层 `tests/` 与 `ops/` 首次纳入 main 历史，避免 release-gate 只在本地物理文件存在时成立。
- [x] `.tmp/`、`.superpowers/`、`.codex-tasks/`、`.gstack/`、`artifacts/`、`frontend/`、`docs/MMMail*` 已迁到仓库外本地归档目录。
- [x] 根 `.gitignore` 覆盖 `frontend-admin` 生成物、依赖缓存、开发期残留和历史快照，防止 node_modules / CI 产物误入仓。

## 10. 一句话总结

**v2.1.3 收尾已把 v2.1.2 剩余缺口转为可执行 release-gate：Docker 数据层 e2e、oxfmt 硬阻断、CI release-gate 与 v2.1.4 范围归宿均已落档。**
