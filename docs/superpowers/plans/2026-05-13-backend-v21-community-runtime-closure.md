# Backend v2.1 Community Runtime Closure 实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 为 v2.1 Workspace、Settings、Entitlements 补齐 Community 运行时，让 `frontend-v2` 已声明的 `/api/v2` 客户端调用真实后端端点，不再依赖占位响应或未实现路由。

**架构：** 新增三个薄 controller：`V21WorkspaceController`、`V21SettingsController`、`V21EntitlementsController`。每个 controller 只做鉴权用户上下文和 HTTP 绑定，实际运行时逻辑放入对应 bridge service。Workspace 复用 v2 Collaboration 持久化任务和活动；Settings 复用 `UserPreferenceService` 与 `AuthService`；Entitlements 从 `V21ApiContractCatalog` 派生可用和锁定状态。Unsupported write 必须显式返回 `INVALID_ARGUMENT`，不能伪造保存成功。

**技术栈：** Java 21、Spring Boot 3.5、MockMvc、JUnit 5、AssertJ、Maven、Vue 3 `frontend-v2` service contract tests、pnpm。

**执行状态：** planned on 2026-05-13.

---

## 设计输入

- 方案文档：`docs/superpowers/specs/2026-05-13-backend-v21-community-runtime-closure-design.md`
- 前端 Workspace 契约：`frontend-v2/src/service/api/workspace.ts`
- 前端 Settings 契约：`frontend-v2/src/service/api/settings.ts`
- 前端 Entitlements 契约：`frontend-v2/src/service/api/entitlements.ts`
- UI 参考分组：
  - Workspace：首页设计图和 `/workspace` 工作台摘要、活动、任务模式。
  - Settings：设置页 profile、security、devices、notifications 面板模式。
  - Entitlements：Admin/entitlements 访问边界和 plan matrix 表达。

## 文件结构

- 创建：`backend/mmmail-server/src/test/java/com/mmmail/server/BackendV21CommunityRuntimeClosureTest.java`
  - 红绿测试，覆盖 Workspace、Settings、Entitlements Community 运行时闭环。
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/controller/V21WorkspaceController.java`
  - `/api/v2/workspace` controller。
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/controller/V21SettingsController.java`
  - `/api/v2/settings` controller。
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/controller/V21EntitlementsController.java`
  - `/api/v2/entitlements` controller。
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/service/V21WorkspaceRuntimeBridgeService.java`
  - Workspace summary、activity、tasks 和 task patch bridge。
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/service/V21SettingsRuntimeBridgeService.java`
  - Settings profile、security、devices、notifications bridge。
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/service/V21EntitlementRuntimeBridgeService.java`
  - Entitlement states 和 matrix 派生逻辑。
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/model/dto/PatchV21WorkspaceTaskRequest.java`
  - Workspace task patch 请求。
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/model/dto/PatchV21SecuritySettingsRequest.java`
  - Security settings patch 请求绑定，当前 Community runtime 显式拒绝持久化。
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/model/dto/PatchV21NotificationSettingsRequest.java`
  - Notification settings patch 请求绑定，当前 Community runtime 显式拒绝持久化。
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/model/vo/V21WorkspaceSummaryProductVo.java`
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/model/vo/V21WorkspaceSummaryVo.java`
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/model/vo/V21WorkspaceActivityItemVo.java`
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/model/vo/V21WorkspaceTaskVo.java`
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/model/vo/V21SecuritySettingsVo.java`
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/model/vo/V21DeviceSessionVo.java`
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/model/vo/V21NotificationSettingsVo.java`
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/model/vo/V21EntitlementStateVo.java`
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/model/vo/V21EntitlementMatrixVo.java`
- 修改：`docs/superpowers/progress/v21-implementation-progress.md`
  - 完成后记录切片、提交和验证证据。

## 任务 1：新增 Community runtime 红测

**文件：**
- 创建：`backend/mmmail-server/src/test/java/com/mmmail/server/BackendV21CommunityRuntimeClosureTest.java`

- [ ] **步骤 1：建立测试类骨架**

使用 `@SpringBootTest`、`@AutoConfigureMockMvc`、`@ActiveProfiles("test")`。复用当前 backend v2 测试风格，提供以下 helper：

- `register(String email, String displayName)`：调用 `/api/v1/auth/register` 并返回 token。
- `login(String email, String password)`：创建第二个 session，用于 devices 测试。
- `readJson(MvcResult result)`：通过 `ObjectMapper` 读取响应。
- `createCollaborationProject(String token)`：调用 `/api/v2/collaboration/projects`。
- `createCollaborationTask(String token, String projectId, String title)`：调用 `/api/v2/collaboration/tasks`。

- [ ] **步骤 2：覆盖 Workspace 真实任务闭环**

新增测试 `workspaceShouldExposeAndPatchRealCollaborationTasks`：

1. 注册用户。
2. 创建 v2 Collaboration project。
3. 创建 v2 Collaboration task。
4. `GET /api/v2/workspace/summary` 断言 `$.code=0`、`$.data.productCards` 非空、`$.data.systemStatus=READY`。
5. `GET /api/v2/workspace/activity` 断言 `$.code=0`，允许空列表但必须存在 `$.data`。
6. `GET /api/v2/workspace/tasks` 断言返回 `collaboration-task-<taskId>`，`completed=false`。
7. `PATCH /api/v2/workspace/tasks/collaboration-task-<taskId>` body 为 `{"completed":true,"title":"Done checklist"}`。
8. 再查 `/api/v2/collaboration/tasks`，断言对应任务 `status=DONE` 且 `title=Done checklist`。
9. `PATCH /api/v2/workspace/tasks/unsupported-1` 断言 HTTP 400 且错误码为 `INVALID_ARGUMENT`。

- [ ] **步骤 3：覆盖 Settings 真实适配和显式拒绝**

新增测试 `settingsShouldBridgeProfileDevicesSecurityAndNotifications`：

1. 注册用户并再次登录，制造至少两个 session。
2. `GET /api/v2/settings/profile` 断言返回真实 profile。
3. `PATCH /api/v2/settings/profile` 使用 `UpdateProfileRequest` 完整字段并断言 display name 持久化。
4. `GET /api/v2/settings/security` 断言 `mfaEnabled=false`、`recoveryEmail=null`。
5. `PATCH /api/v2/settings/security` 断言 HTTP 400 和 `INVALID_ARGUMENT`，因为当前没有真实 MFA/recovery persistence。
6. `GET /api/v2/settings/devices` 断言列表包含 current session 和 non-current session。
7. `DELETE /api/v2/settings/devices/{nonCurrentSessionId}` 后再次 list，断言该 session 消失或不再 active。
8. `DELETE /api/v2/settings/devices/{currentSessionId}` 断言保留 `AuthService` 现有拒绝行为。
9. `GET /api/v2/settings/notifications` 断言 `emailDigest=true`、`productUpdates=true`。
10. `PATCH /api/v2/settings/notifications` 断言 HTTP 400 和 `INVALID_ARGUMENT`，不能回显请求体伪造保存成功。

- [ ] **步骤 4：覆盖 Entitlements catalog 派生**

新增测试 `entitlementsShouldReflectCatalogTiers`：

1. 注册用户。
2. `GET /api/v2/entitlements` 断言包含 `GET /api/v2/workspace/summary`，`state=available`，`requiredPlan=null`。
3. 断言包含 `POST /api/v2/command-center/runs`，`state=locked`，`requiredPlan=premium`。
4. 断言包含 `GET /api/v2/billing/summary`，`state=locked`，`requiredPlan=hosted`。
5. 断言包含 `GET /api/v2/admin/summary`，`state=locked`，`requiredPlan=enterprise-governance`。
6. 断言不包含 public helper identity，例如 public share token helper 和 auth helper routes。
7. `GET /api/v2/entitlements/matrix` 断言 `community` 包含 workspace summary，`premium` 包含 command-center run，`hosted` 包含 billing summary。

- [ ] **步骤 5：运行红测**

运行：

```bash
timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml test -Dtest=BackendV21CommunityRuntimeClosureTest -Dsurefire.failIfNoSpecifiedTests=false
```

预期：命令失败，失败原因应指向 `/api/v2/workspace/*`、`/api/v2/settings/*`、`/api/v2/entitlements/*` controller 尚未实现或响应字段缺失。

- [ ] **步骤 6：提交红测**

```bash
git status --short --branch
git add backend/mmmail-server/src/test/java/com/mmmail/server/BackendV21CommunityRuntimeClosureTest.java
git diff --cached --check
git commit -m "test(backend-v21): cover community runtime closure"
```

## 任务 2：新增前端契约对齐的 DTO 和 VO

**文件：**
- 创建：上述 `PatchV21*Request`、`V21*Vo` records。

- [ ] **步骤 1：新增 Workspace records**

字段必须匹配 `frontend-v2/src/service/api/workspace.ts`：

- `V21WorkspaceSummaryVo(List<V21WorkspaceSummaryProductVo> productCards, int recommendationCount, String systemStatus)`
- `V21WorkspaceSummaryProductVo(String key, String label, String value, String state, LocalDateTime updatedAt)`
- `V21WorkspaceActivityItemVo(String id, String product, String title, LocalDateTime occurredAt, String actor)`
- `V21WorkspaceTaskVo(String id, String title, boolean completed, LocalDateTime dueAt, String product)`
- `PatchV21WorkspaceTaskRequest(Boolean completed, String title)`

- [ ] **步骤 2：新增 Settings records**

字段必须匹配 `frontend-v2/src/service/api/settings.ts`：

- `V21SecuritySettingsVo(boolean mfaEnabled, String recoveryEmail)`
- `PatchV21SecuritySettingsRequest(Boolean mfaEnabled, String recoveryEmail)`
- `V21DeviceSessionVo(String id, String deviceName, LocalDateTime lastActiveAt, boolean current)`
- `V21NotificationSettingsVo(boolean emailDigest, boolean productUpdates)`
- `PatchV21NotificationSettingsRequest(Boolean emailDigest, Boolean productUpdates)`

- [ ] **步骤 3：新增 Entitlement records**

字段必须匹配 `frontend-v2/src/service/api/entitlements.ts`：

- `V21EntitlementStateVo(String key, String label, String state, String requiredPlan)`
- `V21EntitlementMatrixVo(List<String> community, List<String> premium, List<String> hosted)`

`enterprise-governance` 只在 entitlement list 中通过 `requiredPlan` 暴露；matrix 维持前端现有三列形态，不扩展新字段。

- [ ] **步骤 4：运行编译级测试**

```bash
timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml test -Dtest=BackendV21CommunityRuntimeClosureTest -Dsurefire.failIfNoSpecifiedTests=false
```

预期：测试仍失败，但不应再因为新增 record 类型编译失败。

- [ ] **步骤 5：提交 records**

```bash
git status --short --branch
git add backend/mmmail-server/src/main/java/com/mmmail/server/model/dto/PatchV21WorkspaceTaskRequest.java backend/mmmail-server/src/main/java/com/mmmail/server/model/dto/PatchV21SecuritySettingsRequest.java backend/mmmail-server/src/main/java/com/mmmail/server/model/dto/PatchV21NotificationSettingsRequest.java backend/mmmail-server/src/main/java/com/mmmail/server/model/vo/V21WorkspaceSummaryProductVo.java backend/mmmail-server/src/main/java/com/mmmail/server/model/vo/V21WorkspaceSummaryVo.java backend/mmmail-server/src/main/java/com/mmmail/server/model/vo/V21WorkspaceActivityItemVo.java backend/mmmail-server/src/main/java/com/mmmail/server/model/vo/V21WorkspaceTaskVo.java backend/mmmail-server/src/main/java/com/mmmail/server/model/vo/V21SecuritySettingsVo.java backend/mmmail-server/src/main/java/com/mmmail/server/model/vo/V21DeviceSessionVo.java backend/mmmail-server/src/main/java/com/mmmail/server/model/vo/V21NotificationSettingsVo.java backend/mmmail-server/src/main/java/com/mmmail/server/model/vo/V21EntitlementStateVo.java backend/mmmail-server/src/main/java/com/mmmail/server/model/vo/V21EntitlementMatrixVo.java
git diff --cached --check
git commit -m "feat(backend-v21): add community runtime records"
```

## 任务 3：实现 Workspace runtime bridge

**文件：**
- 创建：`V21WorkspaceRuntimeBridgeService.java`
- 创建：`V21WorkspaceController.java`

- [ ] **步骤 1：实现 service 常量和 ID 解析**

在 service 中定义：

- `TASK_SOURCE_COLLABORATION = "collaboration-task-"`
- `SYSTEM_STATUS_READY = "READY"`
- `ENTITLEMENT_STATE_COMMUNITY = "community"`

`parseCollaborationTaskId(String id)` 必须：

- 拒绝 blank id。
- 拒绝非 `collaboration-task-` 前缀。
- 拒绝前缀后不是数字的 id。
- 统一抛 `BizException(ErrorCode.INVALID_ARGUMENT, ...)`。

- [ ] **步骤 2：实现 summary**

`summary(Long userId)`：

1. 从 `V21OpsRuntimeBridgeService.listProjects(userId)`、`listTasks(userId)`、`listActivity(userId)` 读取真实数据。
2. 按 product 汇总 product cards。
3. `key` 使用小写 product。
4. `label` 使用原 product 名。
5. `value` 使用 `<taskCount> tasks`。
6. `state` 固定为 `community`。
7. `updatedAt` 使用该 product 最新 activity 时间；没有 activity 时为 `null`。
8. `recommendationCount` 使用活动数量。
9. `systemStatus` 固定为 `READY`。

- [ ] **步骤 3：实现 activity 和 tasks**

`activity(Long userId)`：

- 映射 `V21CollaborationActivityVo` 到 `V21WorkspaceActivityItemVo`。
- `actor` 当前没有真实字段时返回 `null`，不能硬编码演示用户。

`tasks(Long userId)`：

- 映射 `V21CollaborationTaskVo`。
- id 输出 `collaboration-task-<rawId>`。
- `completed` 对 `DONE` 和 `ARCHIVED` 返回 true，其他状态 false。
- `dueAt` 和 `product` 保留真实字段。

- [ ] **步骤 4：实现 task patch**

`patchTask(Long userId, String workspaceTaskId, PatchV21WorkspaceTaskRequest request, HttpServletRequest httpRequest)`：

1. 请求体为 null 或 `completed`、`title` 同时为 null 时抛 `INVALID_ARGUMENT`。
2. 解析 `collaboration-task-<id>`。
3. `completed=true` 映射 `DONE`。
4. `completed=false` 映射 `OPEN`。
5. title 传给 `UpdateV21CollaborationTaskRequest`。
6. 调用 `V21OpsRuntimeBridgeService.updateTask(...)`，复用既有协作任务写入和 audit/outbox 逻辑。
7. 返回更新后的 `V21WorkspaceTaskVo`。

- [ ] **步骤 5：实现 controller**

`V21WorkspaceController`：

- `@RequestMapping("/api/v2/workspace")`
- `GET /summary`
- `GET /activity`
- `GET /tasks`
- `PATCH /tasks/{id}`
- 用户 id 从 `SecurityUtils.currentUserId()` 获取。
- 响应使用当前仓库统一 `ApiResponse.success(...)`。

- [ ] **步骤 6：运行 Workspace 目标测试**

```bash
timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml test -Dtest=BackendV21CommunityRuntimeClosureTest -Dsurefire.failIfNoSpecifiedTests=false
```

预期：Workspace 相关断言通过，Settings 和 Entitlements 仍失败。

- [ ] **步骤 7：提交 Workspace runtime**

```bash
git status --short --branch
git add backend/mmmail-server/src/main/java/com/mmmail/server/controller/V21WorkspaceController.java backend/mmmail-server/src/main/java/com/mmmail/server/service/V21WorkspaceRuntimeBridgeService.java
git diff --cached --check
git commit -m "feat(backend-v21): add workspace community runtime"
```

## 任务 4：实现 Settings runtime bridge

**文件：**
- 创建：`V21SettingsRuntimeBridgeService.java`
- 创建：`V21SettingsController.java`

- [ ] **步骤 1：实现 profile 适配**

`getProfile(Long userId)` 直接调用 `UserPreferenceService.getProfile(userId)`。

`updateProfile(Long userId, UpdateProfileRequest request)` 直接调用 `UserPreferenceService.updateProfile(userId, request)`。

- [ ] **步骤 2：实现 security 显式能力状态**

`security()` 返回 `new V21SecuritySettingsVo(false, null)`。

`patchSecurity(PatchV21SecuritySettingsRequest request)` 固定抛 `BizException(ErrorCode.INVALID_ARGUMENT, "v2 security settings patch is not supported by the current Community runtime")`。

- [ ] **步骤 3：实现 devices 适配**

`devices(Long userId, Long currentSessionId)`：

- 调用 `AuthService.listSessions(userId, currentSessionId)`。
- 将 `UserSessionVo.id()` 转成字符串。
- `deviceName` 使用 `current ? "Current device" : "Active session"`。
- `lastActiveAt` 使用 `UserSessionVo.createdAt()`。
- `current` 保留真实字段。

`deleteDevice(Long userId, Long currentSessionId, String deviceId, HttpServletRequest request)`：

- 将 `deviceId` 解析为 Long。
- 非数字抛 `INVALID_ARGUMENT`。
- 调用 `AuthService.revokeSession(userId, currentSessionId, parsedId, request.getRemoteAddr())`。
- 保留 current device 不能 revoke 的现有行为。

- [ ] **步骤 4：实现 notification 显式状态**

`notifications()` 返回 `new V21NotificationSettingsVo(true, true)`。

`patchNotifications(PatchV21NotificationSettingsRequest request)` 固定抛 `BizException(ErrorCode.INVALID_ARGUMENT, "v2 notification settings patch is not persisted by the current Community runtime")`。

- [ ] **步骤 5：实现 controller**

`V21SettingsController`：

- `@RequestMapping("/api/v2/settings")`
- `GET /profile`
- `PATCH /profile`
- `GET /security`
- `PATCH /security`
- `GET /devices`
- `DELETE /devices/{id}`
- `GET /notifications`
- `PATCH /notifications`
- 用户 id 从 `SecurityUtils.currentUserId()` 获取。
- 当前 session id 从 `SecurityUtils.currentSessionId()` 获取。
- 所有成功响应使用 `ApiResponse.success(...)`。

- [ ] **步骤 6：运行 Settings 目标测试**

```bash
timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml test -Dtest=BackendV21CommunityRuntimeClosureTest -Dsurefire.failIfNoSpecifiedTests=false
```

预期：Workspace 和 Settings 相关断言通过，Entitlements 仍失败。

- [ ] **步骤 7：提交 Settings runtime**

```bash
git status --short --branch
git add backend/mmmail-server/src/main/java/com/mmmail/server/controller/V21SettingsController.java backend/mmmail-server/src/main/java/com/mmmail/server/service/V21SettingsRuntimeBridgeService.java
git diff --cached --check
git commit -m "feat(backend-v21): add settings community runtime"
```

## 任务 5：实现 Entitlements runtime bridge

**文件：**
- 创建：`V21EntitlementRuntimeBridgeService.java`
- 创建：`V21EntitlementsController.java`

- [ ] **步骤 1：实现 catalog 过滤**

从 `V21ApiContractCatalog.defaultCatalog().contracts()` 读取 contract。

排除 public helper owner modules：

- `identity`
- `public-share`
- `system`

保留 Workspace、Settings、Entitlements、Collaboration、Command Center、Notifications、Billing、Admin Governance 等产品或能力面 contract。

- [ ] **步骤 2：实现 entitlement state 映射**

对每个保留 contract：

- `key = contract.identity()`
- `label = contract.ownerModule() + " " + contract.path()`
- `community` entitlement 输出 `state=available`、`requiredPlan=null`
- `premium` entitlement 输出 `state=locked`、`requiredPlan=premium`
- `hosted` entitlement 输出 `state=locked`、`requiredPlan=hosted`
- `enterprise-governance` entitlement 输出 `state=locked`、`requiredPlan=enterprise-governance`

结果按 `ownerModule`、`path`、`method` 稳定排序。

- [ ] **步骤 3：实现 matrix 映射**

`matrix()` 输出三列：

- `community`：所有 community contract identity。
- `premium`：所有 premium contract identity。
- `hosted`：所有 hosted contract identity。

`enterprise-governance` 不塞入 `hosted`，避免 UI 表达误导；治理能力仍在 list endpoint 中以 locked row 展示。

- [ ] **步骤 4：实现 controller**

`V21EntitlementsController`：

- `@RequestMapping("/api/v2/entitlements")`
- `GET ""`
- `GET "/matrix"`
- 用户上下文只用于通过 access gate；service 当前从 catalog 派生全局状态。
- 响应使用 `ApiResponse.success(...)`。

- [ ] **步骤 5：运行完整新测试**

```bash
timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml test -Dtest=BackendV21CommunityRuntimeClosureTest -Dsurefire.failIfNoSpecifiedTests=false
```

预期：`BackendV21CommunityRuntimeClosureTest` 全部通过。

- [ ] **步骤 6：提交 Entitlements runtime**

```bash
git status --short --branch
git add backend/mmmail-server/src/main/java/com/mmmail/server/controller/V21EntitlementsController.java backend/mmmail-server/src/main/java/com/mmmail/server/service/V21EntitlementRuntimeBridgeService.java
git diff --cached --check
git commit -m "feat(backend-v21): add entitlement community runtime"
```

## 任务 6：回归验证、进度记录和收尾提交

**文件：**
- 修改：`docs/superpowers/progress/v21-implementation-progress.md`

- [ ] **步骤 1：运行 backend 回归**

```bash
timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml test -Dtest=BackendV21CommunityRuntimeClosureTest,BackendV21CollaborationWriteRuntimeTest,BackendV21OpsRuntimeBridgeTest,BackendV21AccessEntitlementGatesTest,BackendV21ApiContractCatalogTest -Dsurefire.failIfNoSpecifiedTests=false
```

必须确认退出码为 0，才能进入提交。

- [ ] **步骤 2：运行 frontend 契约回归**

```bash
timeout 60s pnpm --dir frontend-v2 test -- tests/v21-core-workspaces-contract.test.mjs tests/v21-admin-governance-billing-entitlements-contract.test.mjs
```

该命令用于确认 frontend service 契约和 route boundary 没有被 backend runtime 方案破坏。若测试脚本不支持文件参数，改运行：

```bash
timeout 60s pnpm --dir frontend-v2 test
```

- [ ] **步骤 3：更新进度文档**

更新 `docs/superpowers/progress/v21-implementation-progress.md`：

- `Latest backend implementation commit` 改为本切片最终实现提交。
- Completed v2.1 Slices 增加 `Backend Community runtime closure (backend-v21-community-runtime-closure)`。
- Latest Completed Backend Slice 改为 `backend-v21-community-runtime-closure`。
- 写入本切片 commits、files changed、verification commands。
- Active Backend Slice 同步为 completed。

- [ ] **步骤 4：提交进度文档**

```bash
git status --short --branch
git add docs/superpowers/progress/v21-implementation-progress.md
git diff --cached --check
git commit -m "docs(backend-v21): record community runtime closure progress"
```

- [ ] **步骤 5：最终状态检查**

```bash
git status --short --branch
git log --oneline -6
```

确认只剩用户既有未跟踪路径或明确不纳入提交的路径。

## 验收标准

- 所有 in-scope `/api/v2/workspace/*` endpoint 都有真实 controller 方法。
- 所有 in-scope `/api/v2/settings/*` endpoint 都有真实 controller 方法。
- 所有 in-scope `/api/v2/entitlements/*` endpoint 都有真实 controller 方法。
- Workspace task patch 只接受 `collaboration-task-<id>`，并写回真实 Collaboration task。
- Settings profile 写入真实用户偏好。
- Settings device delete 复用真实 session revoke。
- Security 和 notification patch 在没有真实 persistence 时显式失败，不回显请求体假装成功。
- Entitlements list 和 matrix 从 `V21ApiContractCatalog` 派生。
- Premium、hosted、enterprise-governance 能力保持 locked。
- 不新增 mock success、silent fallback、吞异常返回成功、临时 cap 或防御式绕行。
- 新增 Java 文件遵守本仓库 code metrics，单文件不超过 500 行，函数不超过 50 行。
- 目标 backend 测试和指定 frontend 契约回归通过。

## 推荐执行顺序

1. 先执行任务 1，提交红测。
2. 执行任务 2，提交 records。
3. 执行任务 3，提交 Workspace runtime。
4. 执行任务 4，提交 Settings runtime。
5. 执行任务 5，提交 Entitlements runtime。
6. 执行任务 6，提交进度文档并做最终状态检查。
