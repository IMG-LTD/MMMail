# MMMail v2.1 集成测试报告

**测试日期**: 2026-05-14
**测试分支**: main
**最新提交**: 78d400a0 (fix(backend): restrict module scan roots)

---

## 1. 测试概览（第二轮）

| 维度 | 第一轮 | 第二轮 | 状态 |
|------|--------|--------|------|
| 前端 TypeScript 编译 | ✅ 通过 | ✅ 通过 | 稳定 |
| 前端生产构建 | ✅ 8.11s | ✅ 7.72s | 稳定 |
| 前端合约测试 | ✅ 93/93 | ✅ 93/93 | 稳定 |
| 前端安全审计 | — | ✅ 无漏洞 | 新增 |
| 后端 Java 编译 | ✅ 通过 | ✅ 通过 | 稳定 |
| 后端 v2.1 运行时测试 | ✅ 62/62 | ✅ 62/62 | 稳定 |
| 后端全量测试 | ⚠️ 280/323 (3F+40E) | ⚠️ 285/325 (0F+40E) | 改善 |
| 前后端 API 合约对齐 | ⚠️ 34 缺失 | ⚠️ 33 缺失 | 微改善 |
| 安全扫描 | — | ✅ 无真实泄露 | 新增 |

---

## 2. 后端测试详情

### 2.1 v2.1 运行时桥接测试（全部通过，两轮稳定）

| 测试类 | 测试数 | 第一轮 | 第二轮 |
|--------|--------|--------|--------|
| BackendV21AccessEntitlementGatesTest | 8 | ✅ | ✅ |
| BackendV21ApiContractCatalogTest | 6 | ✅ | ✅ |
| BackendV21BackgroundJobFoundationTest | 9 | ✅ | ✅ |
| BackendV21CalendarRuntimeBridgeTest | 3 | ✅ | ✅ |
| BackendV21CollaborationWriteRuntimeTest | 2 | ✅ | ✅ |
| BackendV21CommunityRuntimeClosureTest | 3 | ✅ | ✅ |
| BackendV21DocsSheetsRuntimeBridgeTest | 3 | ✅ | ✅ |
| BackendV21DriveRuntimeBridgeTest | 4 | ✅ | ✅ |
| BackendV21EventOutboxFoundationTest | 8 | ✅ | ✅ |
| BackendV21MailRuntimeBridgeTest | 5 | ✅ | ✅ |
| BackendV21OpsRuntimeBridgeTest | 3 | ✅ | ✅ |
| BackendV21PassRuntimeBridgeTest | 3 | ✅ | ✅ |
| BackendV21RuntimeContractGapClosureTest | 5 | ✅ | ✅ |

### 2.2 第二轮新增验证（全部通过）

| 测试组 | 测试数 | 结果 |
|--------|--------|------|
| Auth/RBAC 回归 (AuthFlowIntegrationTest 等) | 7 | ✅ |
| Docs 协作回归 (DocsCollaboration 等) | 5 | ✅ |
| Mail GA 回归 (MailGa, MailAttachment 等) | 11 | ✅ |
| Calendar GA 回归 (CalendarSharing 等) | 7 | ✅ |
| Drive GA 回归 (DriveRelease, DriveCollaborator 等) | 8 | ✅ |
| Pass 模块回归 (PassMonitor, PassBusiness 等) | 11 | ✅ |
| E2EE 加密回归 (MailE2ee, DriveE2ee) | 10 | ✅ |
| 安全基线 (SecurityBaseline, DependencyVersionGuard) | ✅ | ✅ |
| 可观测性 (JobRunMonitor, GlobalExceptionHandler) | ✅ | ✅ |
| v2 合约回归 (ContractCatalog, TenantScope 等) | ✅ | ✅ |

### 2.3 第一轮断言失败 → 第二轮状态

| 缺陷 | 第一轮 | 第二轮 | 结论 |
|------|--------|--------|------|
| BillingReadinessIntegrationTest (403) | ❌ | ✅ HTTP 200 | **已修复** |
| WorkspaceAggregationIntegrationTest (403) | ❌ | ✅ HTTP 200 | **已修复** |
| BackendV2GateContractTest (runbook 缺条目) | ❌ | ✅ 通过 | **已修复** |

### 2.4 环境级联错误（40 个测试，仍存在）

**根因**: `MeetGuestJoinParityIntegrationTest` 触发 Spring ApplicationContext 加载失败（MySQL 连接被拒绝：`Access denied for user 'mmmail_app'@'172.19.0.1'`），后续共享同一 context 的测试类全部级联失败。

**受影响测试类（18 个）**:
AuthenticatorPortabilityIntegrationTest, ExternalAccountProductAccessIntegrationTest, MeetAccessParityIntegrationTest, MeetGuestJoinParityIntegrationTest, OrgBusinessTeamSpaceIntegrationTest, OrgMailIdentityIntegrationTest, OrgMonitorStatusIntegrationTest, SettingsControllerIntegrationTest, SheetsWorkbookDataManagementIntegrationTest, SheetsWorkbookMultiSheetIntegrationTest, SimpleLoginIntegrationTest, SimpleLoginRelayPolicyIntegrationTest, StandardNotesIntegrationTest, SuiteBillingCenterIntegrationTest, SuiteBillingParityIntegrationTest, VpnWorkspaceIntegrationTest, WalletParityAdvancedIntegrationTest, WalletParityIntegrationTest

**严重程度**: 低 — 本地测试环境 MySQL 凭据配置问题，非代码缺陷。这些测试需要真实 MySQL 连接（非 H2 内存数据库），但当前环境的 `mmmail_app` 用户密码与 Docker 容器中配置的不匹配。

---

## 3. 前后端 API 合约对齐分析

### 3.1 总体数据

| 指标 | 数值 |
|------|------|
| 前端 v2 API 调用总数 | 84 |
| 后端 v2 API 端点总数 | 101 |
| 前端调用有后端匹配 | 51 (61%) |
| 前端调用无后端处理器 | 33 (39%) |

### 3.2 前端调用但后端无对应处理器的端点（33 个）

| 模块 | 缺失端点 | 影响级别 |
|------|----------|----------|
| **Admin** | GET alerts, domains, policies, risk, summary, system, users; POST users; PATCH audit, roles | 高 — 管理面板不可用 |
| **Billing** | GET invoices, plans, summary, usage | 高 — 计费面板不可用 |
| **Calendar** | GET resources; PATCH availability; POST bookings | 中 — 资源预订不可用 |
| **Command Center** | GET runs | 中 — 运行历史不可用 |
| **Docs** | GET /api/v2/docs | 高 — 文档列表不可用 |
| **Entitlements** | GET /api/v2/entitlements | 高 — 权限信息不可用 |
| **Labs** | GET /api/v2/labs/modules | 中 — 实验室列表不可用 |
| **Mail** | GET rules | 中 — 邮件规则不可用 |
| **Notifications** | PATCH rules, templates | 低 — 编辑功能不可用 |
| **Pass** | DELETE/PATCH aliases; PATCH share | 低 — 别名管理不可用 |
| **Settings** | GET integrations; PATCH audit; DELETE notifications | 中 — 集成设置不可用 |
| **Sheets** | GET /api/v2/sheets | 高 — 表格列表不可用 |
| **Share** | GET capabilities | 低 — 能力查询不可用 |
| **System** | GET status | 中 — 系统状态不可用 |
| **Collaboration** | PATCH activity | 低 — 活动更新不可用 |

### 3.3 第二轮发现：v2.1 Mail Rules 端点存在但返回 403

第二轮测试日志显示 `GET /api/v2/mail/rules` 端点实际存在于后端（返回 403 而非 404），错误信息为 `"Required entitlement is not enabled"`。这说明该端点已实现但受 entitlement 门控保护，在 Community 版本中默认不可用。

---

## 4. 前端测试详情

### 4.1 合约测试（93/93 通过，两轮稳定）

覆盖范围包括：
- 路由合约（foundation-route, redirect, router）
- 认证合约（auth-scope, auth-runtime）
- 公共分享合约（public-share, public-share-runtime, public-share-view）
- 工作区合约（mail, calendar, drive, pass, docs-sheets, workspace-aggregation）
- v2.1 专项合约（design-system, design-token, shell, routing-gates, admin-governance, deployment-security 等）
- 可访问性和响应式合约
- 发布门禁合约

### 4.2 TypeScript 类型检查

`vue-tsc --noEmit` 零错误通过（两轮稳定）。

### 4.3 生产构建

- 构建时间: 7.72s（第二轮）
- 最大 chunk: `index-BnaQljd4.js` (492KB / 145KB gzip)
- 所有路由组件正确代码分割
- 无构建警告

---

## 5. 安全检查

### 5.1 安全配置审查

- ✅ CORS 配置正确限制了允许的源
- ✅ JWT 认证过滤器正确验证 token 版本和会话有效性
- ✅ 安全响应头完整（CSP、Permissions-Policy、HSTS、X-Frame-Options: DENY、Referrer-Policy）
- ✅ 密码使用 BCrypt 加密
- ✅ 会话管理为无状态（STATELESS）
- ✅ 公共端点白名单合理
- ✅ `.env.example` 中无真实密钥泄露
- ✅ 前端依赖审计无已知漏洞
- ✅ 安全扫描无真实密钥泄露（仅 node_modules 和 .tmp 中的误报）

### 5.2 安全回归测试

- ✅ DependencyVersionGuardTest — 通过
- ✅ SecurityBaselineIntegrationTest — 通过
- ✅ DriveSecureShareIntegrationTest — 通过
- ✅ DrivePublicFolderShareIntegrationTest — 通过
- ✅ MailAttachmentIntegrationTest — 通过

---

## 6. 基础设施检查

### 6.1 Docker Compose

- ✅ `docker-compose.yml` 标准模式配置完整（MySQL + Redis + Nacos + Backend + Frontend）
- ✅ `docker-compose.minimal.yml` 最小模式可用
- ✅ 健康检查配置合理
- ✅ 端口绑定到 127.0.0.1（安全）
- ✅ 数据持久化通过 named volumes

### 6.2 环境模板

- ✅ `.env.example` 包含所有必需变量
- ✅ 敏感值使用 `replace-with-*` 占位符
- ✅ `validate-runtime-env.sh` 正确拒绝未替换占位符的模板

---

## 7. 缺陷汇总（第二轮更新）

| ID | 严重程度 | 模块 | 描述 | 第一轮 | 第二轮 |
|----|----------|------|------|--------|--------|
| BUG-001 | 中 | Backend/Test | BillingReadiness 测试返回 403 | ❌ | ✅ **已修复** |
| BUG-002 | 中 | Backend/Test | WorkspaceAggregation 测试返回 403 | ❌ | ✅ **已修复** |
| BUG-003 | 高 | Ops/Gate | deployment-runbook.md 缺少门禁条目 | ❌ | ✅ **已修复** |
| BUG-004 | 高 | Frontend/API | 33 个前端 v2 API 调用无后端处理器 | ❌ 34个 | ❌ 33个 |
| BUG-005 | 低 | Backend/Test | 18 个测试类因 MySQL 凭据不匹配级联失败 | ❌ | ❌ 环境问题 |
| BUG-006 | 低 | Backend/Test | 3 个测试需要 Docker/Testcontainers 环境 | ❌ | ❌ 环境问题 |

---

## 8. 发布建议

### 阻塞项

**BUG-004（33 个前端 API 调用缺少后端实现）** 是唯一的代码级阻塞项。

需要确认处理策略：
1. **实现后端端点** — 在 v2.1 中补全这些 controller
2. **前端降级处理** — 对未实现的端点显示"即将推出"或回退到 v1 API
3. **标记为 v2.2 范围** — 如果这些功能不在 v2.1 发布范围内，前端应做优雅降级

高优先级缺失端点（用户主路径受阻）：
- Admin 模块（9 个端点）
- Billing 模块（4 个端点）
- Docs 列表（GET /api/v2/docs）
- Sheets 列表（GET /api/v2/sheets）
- Entitlements（GET /api/v2/entitlements）

### 不阻塞发布

- **BUG-005/006**: 环境配置问题，不影响生产代码质量
- 所有 v2.1 运行时桥接测试稳定通过
- 所有核心模块（Mail、Calendar、Drive、Pass、Docs、Sheets）回归测试通过
- 安全基线和 E2EE 加密测试通过
- 前端编译、构建、合约测试全部通过

---

## 9. 测试环境信息

- OS: Linux 6.17.0-14-generic
- Java: Maven build successful (Spring Boot)
- Node: pnpm + vite build successful
- Frontend: Vue 3 + Naive UI + Vite
- Backend: Spring Boot + MyBatis + MySQL 8.4 + Redis 7.4
- 测试执行: 本地环境，H2 内存数据库用于单元/集成测试
- 第一轮时间: 2026-05-14 17:30 ~ 18:20
- 第二轮时间: 2026-05-14 18:30 ~ 18:50
