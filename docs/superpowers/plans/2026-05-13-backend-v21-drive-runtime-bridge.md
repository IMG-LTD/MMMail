# Backend v2.1 Drive Runtime Bridge 实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 为 v2.1 Drive 增加真实 `/api/v2/drive/*` 运行时桥接，并清理本切片触碰到的无用 Drive 客户端代码。

**架构：** 新增小型 v2 Drive controller 与 runtime bridge service，复用现有 `DriveService` 的真实业务行为。合同层补齐 `GET /api/v2/drive/files/:id/share`，并把 upload read response 从队列语义对齐为 `DriveItem`。前端只补齐 Drive API client 契约和清理未使用 helper，不引入 v1 fallback。

**技术栈：** Spring Boot, MockMvc, MyBatis Plus, Java records, Vue 3, TypeScript, Node test runner, pnpm.

---

## 文件结构

- 创建：`backend/mmmail-server/src/test/java/com/mmmail/server/BackendV21DriveRuntimeBridgeTest.java`：v2 Drive runtime bridge 集成测试。
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/model/dto/V21DriveUploadRequest.java`：v2 JSON upload metadata 请求。
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/service/V21DriveRuntimeBridgeService.java`：v2 payload 到现有 `DriveService` 的适配、显式输入校验、单项读取映射。
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/controller/V21DriveController.java`：`/api/v2/drive/*` controller。
- 修改：`backend/mmmail-platform/src/main/java/com/mmmail/platform/contract/V21ApiContractCatalog.java`：新增 Drive share GET 合同，修正 upload read response。
- 修改：`contracts/openapi/v21-api-catalog.yaml`：新增 Drive share GET OpenAPI 条目。
- 修改：`backend/mmmail-server/src/test/java/com/mmmail/server/BackendV21ApiContractCatalogTest.java`：冻结新增合同和 catalog 数量。
- 修改：`frontend-v2/src/service/api/drive.ts`：删除无合同支撑的未使用 `readDriveFile`，补齐 v2 bridge client 方法。
- 修改：`frontend-v2/tests/drive-workspace-contract.test.mjs`：冻结 Drive client 契约和清理结果。
- 修改：`docs/superpowers/progress/v21-implementation-progress.md`：记录 active/completed backend slice。

## 任务 1：红灯测试和活动进度

**文件：**
- 创建：`backend/mmmail-server/src/test/java/com/mmmail/server/BackendV21DriveRuntimeBridgeTest.java`
- 修改：`backend/mmmail-server/src/test/java/com/mmmail/server/BackendV21ApiContractCatalogTest.java`
- 修改：`docs/superpowers/progress/v21-implementation-progress.md`

- [ ] **步骤 1：记录 active backend slice**

替换 `docs/superpowers/progress/v21-implementation-progress.md` 的 `## Active Backend Slice`：

```markdown
## Active Backend Slice

- Slice: `backend-v21-drive-runtime-bridge`
- Status: `in_progress`
- Started: `2026-05-13`
- Scope: v2 Drive runtime bridge, Drive API contract alignment, frontend Drive client cleanup
- Verification target:
  - `BackendV21DriveRuntimeBridgeTest`
  - `DriveReleaseBlockingIntegrationTest`
  - `BackendV21AccessEntitlementGatesTest`
  - `BackendV21ApiContractCatalogTest`
  - `pnpm --dir frontend-v2 test`
  - `pnpm --dir frontend-v2 typecheck`
  - `pnpm --dir frontend-v2 build`
```

- [ ] **步骤 2：新增 `BackendV21DriveRuntimeBridgeTest`**

测试类必须使用 `@SpringBootTest`、`@AutoConfigureMockMvc`、`@ActiveProfiles("test")`，包含 `MockMvc`、`ObjectMapper`、`PASSWORD = "Password@123"`，并提供 `register()`、`createV1Folder()`、`createV21Upload()`、`readJson()` helper，helper 结构复用 `DriveReleaseBlockingIntegrationTest`。

新增测试方法 `v21DriveShouldUseRuntimeDriveStateForCommunityPaths()`，按顺序断言：

```java
String token = register("v21-drive-owner-" + suffix + "@mmmail.local", "V21 Drive Owner");
String folderId = createV1Folder(token, "v21-root", null);
String fileId = createV21Upload(token, "v21-plan.txt", folderId, 42);

mockMvc.perform(get("/api/v2/drive/uploads/" + fileId).header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.id").value(fileId))
        .andExpect(jsonPath("$.data.name").value("v21-plan.txt"));
```

同一测试继续覆盖：

```java
GET /api/v2/drive/files?parentId={folderId} -> $.data[0].id == fileId
GET /api/v2/drive/folders -> $.data[0].id == folderId
GET /api/v2/drive/storage/summary -> fileCount=1, folderCount=1, storageBytes=42
GET /api/v2/drive/files/{fileId}/share -> $.data.length() == 0
POST /api/v2/drive/files/{fileId}/share with {"permission":"VIEW","password":"Share#123"} -> itemId=fileId
GET /api/v2/drive/files/{fileId}/share -> $.data.length() == 1
PATCH /api/v2/drive/files/{fileId} with {"name":"v21-plan-renamed.txt","parentId":null} -> name updated
DELETE /api/v2/drive/files/{fileId} -> code=0
```

新增测试方法 `v21DriveShouldRejectEmptyPatchAndKeepVersionsPremiumGated()`：

```java
String token = register("v21-drive-gate-" + System.nanoTime() + "@mmmail.local", "V21 Drive Gate");
String fileId = createV21Upload(token, "v21-gated.bin", null, 7);

mockMvc.perform(patch("/api/v2/drive/files/" + fileId)
        .header("Authorization", "Bearer " + token)
        .contentType(MediaType.APPLICATION_JSON)
        .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_ARGUMENT.getCode()));

mockMvc.perform(get("/api/v2/drive/files/" + fileId + "/versions")
        .header("Authorization", "Bearer " + token))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value(ErrorCode.V2_ENTITLEMENT_REQUIRED.getCode()));
```

- [ ] **步骤 3：更新 catalog 测试断言**

在 `catalogShouldCoverV21NamespacesWithMetadata()` 中加入：

```java
assertContract(byPath, "GET /api/v2/drive/uploads/:id", "drive", "DriveItem", "community");
assertContract(byPath, "GET /api/v2/drive/files/:id/share", "drive", "DriveShareLink[]", "community");
```

在 `authenticatedPlatformEndpointShouldExposeCatalog()` 中把合同数量改为：

```java
.andExpect(jsonPath("$.data.contracts.length()").value(127))
```

在 `openApiCatalogShouldFreezeV21NamespaceCoverage()` 中加入：

```java
.contains("/api/v2/drive/files/{id}/share:")
.contains("List drive shares")
```

- [ ] **步骤 4：运行红灯验证**

```bash
timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml test -Dtest=BackendV21DriveRuntimeBridgeTest,BackendV21ApiContractCatalogTest -Dsurefire.failIfNoSpecifiedTests=false
```

预期：FAIL。失败应指向缺少 Drive v2 合同或 controller，不应是编译错误。

## 任务 2：补齐 v2 Drive 合同

**文件：**
- 修改：`backend/mmmail-platform/src/main/java/com/mmmail/platform/contract/V21ApiContractCatalog.java`
- 修改：`contracts/openapi/v21-api-catalog.yaml`
- 修改：`backend/mmmail-server/src/test/java/com/mmmail/server/BackendV21ApiContractCatalogTest.java`

- [ ] **步骤 1：更新 Java runtime catalog**

在 `driveContracts()` 中把 upload read response 改为：

```java
{"GET", "/api/v2/drive/uploads/:id", "DriveItem", COMMUNITY, "drive:read"},
```

在 share POST 前插入：

```java
{"GET", "/api/v2/drive/files/:id/share", "DriveShareLink[]", COMMUNITY, "drive:share"},
```

- [ ] **步骤 2：更新 OpenAPI catalog**

把 `/api/v2/drive/files/{id}/share:` 改为同时包含 GET 和 POST：

```yaml
  /api/v2/drive/files/{id}/share:
    get: {summary: List drive shares, x-permission: ["drive:share"], x-entitlement: community, x-design-source: docs/MMMail/UI/云盘, responses: {"200": {description: ok}}}
    post: {summary: Share drive file, x-permission: ["drive:share"], x-entitlement: community, x-design-source: docs/MMMail/UI/云盘, responses: {"200": {description: ok}}}
```

- [ ] **步骤 3：运行合同绿灯验证**

```bash
timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml test -Dtest=BackendV21ApiContractCatalogTest,BackendV21AccessEntitlementGatesTest -Dsurefire.failIfNoSpecifiedTests=false
```

预期：PASS。`BackendV21DriveRuntimeBridgeTest` 仍可失败，因为 controller 尚未实现。

- [ ] **步骤 4：提交合同切片**

```bash
git status --short --branch
git add backend/mmmail-platform/src/main/java/com/mmmail/platform/contract/V21ApiContractCatalog.java contracts/openapi/v21-api-catalog.yaml backend/mmmail-server/src/test/java/com/mmmail/server/BackendV21ApiContractCatalogTest.java
git diff --cached --check
git diff --cached --stat
git commit -m "feat(backend-v21): add drive share read contract"
```

## 任务 3：实现后端 v2 Drive runtime bridge

**文件：**
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/model/dto/V21DriveUploadRequest.java`
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/service/V21DriveRuntimeBridgeService.java`
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/controller/V21DriveController.java`
- 测试：`backend/mmmail-server/src/test/java/com/mmmail/server/BackendV21DriveRuntimeBridgeTest.java`

- [ ] **步骤 1：创建 v2 upload DTO**

`V21DriveUploadRequest.java` 内容：

```java
package com.mmmail.server.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record V21DriveUploadRequest(
        @NotBlank @Size(max = 128) String fileName,
        Long parentId,
        @Min(1) Long sizeBytes
) {
}
```

- [ ] **步骤 2：创建 runtime bridge service**

`V21DriveRuntimeBridgeService` 依赖 `DriveService`、`DriveItemMapper`、`DriveShareLinkMapper`、`DriveFileE2eeService`。必须包含这些公开方法：

```java
List<DriveItemVo> listFiles(Long userId, Long parentId, String keyword, Integer limit)
List<DriveItemVo> listFolders(Long userId, Long parentId, String keyword, Integer limit)
DriveUsageVo usage(Long userId, String ipAddress)
DriveItemVo createUpload(Long userId, V21DriveUploadRequest request, String ipAddress)
DriveItemVo readUpload(Long userId, Long itemId)
DriveItemVo updateFile(Long userId, Long itemId, JsonNode payload, String ipAddress)
void deleteFile(Long userId, Long itemId, String ipAddress)
List<DriveShareLinkVo> listShares(Long userId, Long itemId)
DriveShareLinkVo createShare(Long userId, Long itemId, CreateDriveShareRequest request, String ipAddress)
```

核心映射规则：

```java
createUpload -> new CreateDriveFileRequest(fileName, parentId, "application/octet-stream", sizeBytes, null, null)
listFiles -> driveService.listItems(userId, parentId, keyword, "FILE", limit)
listFolders -> driveService.listItems(userId, parentId, keyword, "FOLDER", limit)
deleteFile -> driveService.deleteItem(userId, itemId, ipAddress)
listShares -> driveService.listShares(userId, itemId)
createShare -> driveService.createShare(userId, itemId, request, ipAddress)
```

`updateFile()` 必须用 `JsonNode.has("name")` 和 `JsonNode.has("parentId")` 区分缺失字段与显式 `null`，空对象抛出：

```java
throw new BizException(ErrorCode.INVALID_ARGUMENT, "Drive file update requires name or parentId");
```

`readUpload()` 用 `DriveItemMapper` 读取 `id + ownerId + deleted=0`，找不到时抛出：

```java
throw new BizException(ErrorCode.INVALID_ARGUMENT, "Drive item is not found");
```

`toItemVo()` 使用 `DriveFileE2eeService.toVo(...)` 生成 `DriveItemVo.e2ee`，share count 使用 `DriveShareLinkMapper.selectCount()` 统计 `status = "ACTIVE"`。

- [ ] **步骤 3：创建 v2 Drive controller**

`V21DriveController` 使用 `@RestController`、`@Validated`、`@RequestMapping("/api/v2/drive")`，所有方法通过 `SecurityUtils.currentUserId()` 取用户。必须暴露：

```java
GET /files -> Result<List<DriveItemVo>>
GET /folders -> Result<List<DriveItemVo>>
POST /uploads -> Result<DriveItemVo>
GET /uploads/{itemId} -> Result<DriveItemVo>
PATCH /files/{itemId} -> Result<DriveItemVo>
DELETE /files/{itemId} -> Result<Void>
GET /files/{itemId}/share -> Result<List<DriveShareLinkVo>>
POST /files/{itemId}/share -> Result<DriveShareLinkVo>
GET /storage/summary -> Result<DriveUsageVo>
```

查询参数：`files` 和 `folders` 支持 `parentId`、`keyword` 默认空字符串、`limit`。

- [ ] **步骤 4：运行后端 bridge 验证**

```bash
timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml test -Dtest=BackendV21DriveRuntimeBridgeTest,DriveReleaseBlockingIntegrationTest,BackendV21AccessEntitlementGatesTest -Dsurefire.failIfNoSpecifiedTests=false
```

预期：PASS。

- [ ] **步骤 5：提交后端 runtime bridge**

```bash
git status --short --branch
git add backend/mmmail-server/src/test/java/com/mmmail/server/BackendV21DriveRuntimeBridgeTest.java backend/mmmail-server/src/main/java/com/mmmail/server/model/dto/V21DriveUploadRequest.java backend/mmmail-server/src/main/java/com/mmmail/server/service/V21DriveRuntimeBridgeService.java backend/mmmail-server/src/main/java/com/mmmail/server/controller/V21DriveController.java
git diff --cached --check
git diff --cached --stat
git commit -m "feat(backend-v21): add drive runtime bridge"
```

## 任务 4：补齐前端 Drive client 并清理无用 helper

**文件：**
- 修改：`frontend-v2/src/service/api/drive.ts`
- 修改：`frontend-v2/tests/drive-workspace-contract.test.mjs`

- [ ] **步骤 1：更新 Drive API client**

删除无调用且没有 v2 contract 支撑的函数：

```ts
export function readDriveFile(fileId: string, token: string) {
  return httpClient.get<ApiResponse<DriveItem>>(`/api/v2/drive/files/${fileId}`, { token })
}
```

新增类型和方法：

```ts
export interface DriveFileUpdatePayload {
  name?: string
  parentId?: string | null
}

export interface DriveSharePayload {
  permission: 'VIEW' | 'EDIT'
  expiresAt?: string | null
  password?: string
}

export function readDriveUpload(uploadId: string, token: string) {
  return httpClient.get<ApiResponse<DriveItem>>(`/api/v2/drive/uploads/${uploadId}`, { token })
}

export function updateDriveFile(fileId: string, payload: DriveFileUpdatePayload, token: string) {
  return httpClient.patch<ApiResponse<DriveItem>>(`/api/v2/drive/files/${fileId}`, { body: payload, token })
}

export function deleteDriveFile(fileId: string, token: string) {
  return httpClient.delete<ApiResponse<void>>(`/api/v2/drive/files/${fileId}`, { token })
}

export function createDriveShare(fileId: string, payload: DriveSharePayload, token: string) {
  return httpClient.post<ApiResponse<DriveShareLink>>(`/api/v2/drive/files/${fileId}/share`, { body: payload, token })
}
```

- [ ] **步骤 2：更新 Drive client contract test**

在 `drive-workspace-contract.test.mjs` 中加入：

```js
assert.match(api, /readDriveUpload/)
assert.match(api, /updateDriveFile/)
assert.match(api, /deleteDriveFile/)
assert.match(api, /createDriveShare/)
assert.doesNotMatch(api, /readDriveFile/)
```

- [ ] **步骤 3：运行前端验证**

```bash
pnpm --dir frontend-v2 test
pnpm --dir frontend-v2 typecheck
pnpm --dir frontend-v2 build
```

预期：全部 PASS。

- [ ] **步骤 4：提交前端 client 清理**

```bash
git status --short --branch
git add frontend-v2/src/service/api/drive.ts frontend-v2/tests/drive-workspace-contract.test.mjs
git diff --cached --check
git diff --cached --stat
git commit -m "feat(frontend-v2): align drive client with runtime bridge"
```

## 任务 5：更新进度并完整验证

**文件：**
- 修改：`docs/superpowers/progress/v21-implementation-progress.md`

- [ ] **步骤 1：更新完成进度**

在 `## Completed v2.1 Slices` 表格追加：

```markdown
| Backend Drive runtime bridge (`backend-v21-drive-runtime-bridge`) | `BackendV21DriveRuntimeBridgeTest`, `V21DriveController`, Drive client runtime bridge cleanup |
```

更新 `## Latest Completed Backend Slice` 和 `## Active Backend Slice`，记录：

```markdown
- Slice: `backend-v21-drive-runtime-bridge`
- Status: `completed`
- Started: `2026-05-13`
- Completed: `2026-05-13`
- Scope: v2 Drive runtime bridge, Drive API contract alignment, frontend Drive client cleanup
```

验证列表必须包含：

```markdown
- `timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml test -Dtest=BackendV21DriveRuntimeBridgeTest,DriveReleaseBlockingIntegrationTest,BackendV21AccessEntitlementGatesTest,BackendV21ApiContractCatalogTest -Dsurefire.failIfNoSpecifiedTests=false`: PASS
- `pnpm --dir frontend-v2 test`: PASS
- `pnpm --dir frontend-v2 typecheck`: PASS
- `pnpm --dir frontend-v2 build`: PASS
```

- [ ] **步骤 2：运行完整切片验证**

```bash
timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml test -Dtest=BackendV21DriveRuntimeBridgeTest,DriveReleaseBlockingIntegrationTest,BackendV21AccessEntitlementGatesTest,BackendV21ApiContractCatalogTest -Dsurefire.failIfNoSpecifiedTests=false
pnpm --dir frontend-v2 test
pnpm --dir frontend-v2 typecheck
pnpm --dir frontend-v2 build
```

预期：全部 PASS。只有这些命令退出码为 0 后，才能声明实现完成。

- [ ] **步骤 3：提交进度文档**

```bash
git status --short --branch
git add -f docs/superpowers/progress/v21-implementation-progress.md
git diff --cached --check
git diff --cached --stat
git commit -m "docs(backend-v21): update drive runtime bridge progress"
```

## 验证边界

- 不使用 `git add .` 或 `git add -A`。
- 不暂存 `.superpowers/`、`.tmp/`、`docs/MMMail.zip`、`docs/MMMail/`、`frontend/`。
- 后端测试命令必须带 `timeout 60s`。
- 任何失败都保持显式失败，不添加 mock success、silent fallback、模拟队列成功或宽松 catch。
- `DriveService` 是历史大文件，本计划不把 v2 适配逻辑继续堆进该文件；新增小型 bridge service 承担 v2 映射职责。
