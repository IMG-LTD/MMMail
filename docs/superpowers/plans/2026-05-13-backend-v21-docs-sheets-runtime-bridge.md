# Backend v2.1 Docs and Sheets Runtime Bridge 实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 为 v2.1 Docs 和 Sheets 增加真实 `/api/v2/docs*`、`/api/v2/sheets*` Community 运行时桥接。

**架构：** 新增薄 v2 controller，直接复用现有 Docs 与 Sheets service 的真实业务逻辑。Community 路由返回持久化运行时数据；Premium 路由继续由 v2 entitlement gate 显式阻断；Sheets JSON import 返回明确 `INVALID_ARGUMENT`，不制造假导入结果。

**技术栈：** Spring Boot, MockMvc, Java records, Jakarta Validation, Vue 3, TypeScript, Node test runner, pnpm.

---

## 文件结构

- 创建：`backend/mmmail-server/src/test/java/com/mmmail/server/BackendV21DocsSheetsRuntimeBridgeTest.java`：v2 Docs/Sheets runtime bridge 集成测试。
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/controller/V21DocsController.java`：`/api/v2/docs*` controller，复用 `DocsService` 与 `DocsCollaborationService`。
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/controller/V21SheetsController.java`：`/api/v2/sheets*` controller，复用 `SheetsService`。
- 修改：`docs/superpowers/progress/v21-implementation-progress.md`：记录 active/completed backend slice 与验证证据。

## 任务 1：红灯测试和活动进度

**文件：**
- 创建：`backend/mmmail-server/src/test/java/com/mmmail/server/BackendV21DocsSheetsRuntimeBridgeTest.java`
- 修改：`docs/superpowers/progress/v21-implementation-progress.md`

- [ ] **步骤 1：记录 active backend slice**

替换 `docs/superpowers/progress/v21-implementation-progress.md` 的 `## Active Backend Slice`：

```markdown
## Active Backend Slice

- Slice: `backend-v21-docs-sheets-runtime-bridge`
- Status: `in_progress`
- Started: `2026-05-13`
- Scope: v2 Docs and Sheets runtime bridge, explicit Premium gates, unsupported Sheets JSON import error
- Verification target:
  - `BackendV21DocsSheetsRuntimeBridgeTest`
  - `DocsCollaborationIntegrationTest`
  - `SheetsWorkbookIntegrationTest`
  - `SheetsSharingVersionIntegrationTest`
  - `BackendV21AccessEntitlementGatesTest`
  - `BackendV21ApiContractCatalogTest`
  - `pnpm --dir frontend-v2 test`
  - `pnpm --dir frontend-v2 typecheck`
  - `pnpm --dir frontend-v2 build`
```

- [ ] **步骤 2：创建 red test 类**

创建 `BackendV21DocsSheetsRuntimeBridgeTest.java`，类声明和字段必须是：

```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BackendV21DocsSheetsRuntimeBridgeTest {

    private static final String PASSWORD = "Password@123";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
}
```

需要 imports：`JsonNode`、`ObjectMapper`、`ErrorCode`、`Test`、`Autowired`、`SpringBootTest`、`AutoConfigureMockMvc`、`MediaType`、`ActiveProfiles`、`MockMvc`、`MvcResult`，以及 MockMvc request/result static imports：`get`、`patch`、`post`、`jsonPath`、`status`。

- [ ] **步骤 3：添加 Docs runtime 测试**

在测试类中添加 `v21DocsShouldUseRuntimeStateForCommunityPaths()`，关键断言如下：

```java
String suffix = String.valueOf(System.nanoTime());
String ownerToken = register("v21-docs-owner-" + suffix + "@mmmail.local", "V21 Docs Owner");
register("v21-docs-collab-" + suffix + "@mmmail.local", "V21 Docs Collaborator");
String noteId = createV21Doc(ownerToken, "V21 Docs", "Initial body");

mockMvc.perform(get("/api/v2/docs").header("Authorization", "Bearer " + ownerToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].id").value(noteId));

mockMvc.perform(get("/api/v2/docs/" + noteId).header("Authorization", "Bearer " + ownerToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.title").value("V21 Docs"))
        .andExpect(jsonPath("$.data.currentVersion").value(1));
```

同一测试继续覆盖：

```java
PATCH /api/v2/docs/{noteId} with title/content/currentVersion=1 -> title updated, currentVersion=2
POST /api/v2/docs/{noteId}/comments with excerpt/content -> content returned
GET /api/v2/docs/{noteId}/comments -> data length is 1
POST /api/v2/docs/{noteId}/share with collaboratorEmail/permission EDIT -> permission is EDIT
```

- [ ] **步骤 4：添加 Sheets runtime 与 gate 测试**

添加 `v21SheetsShouldUseRuntimeStateForCommunityPaths()`：

```java
String token = register("v21-sheets-owner-" + System.nanoTime() + "@mmmail.local", "V21 Sheets Owner");
String workbookId = createV21Workbook(token, "V21 Workbook");

mockMvc.perform(get("/api/v2/sheets").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].id").value(workbookId));

MvcResult detailResult = mockMvc.perform(get("/api/v2/sheets/" + workbookId)
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.title").value("V21 Workbook"))
        .andReturn();
String sheetId = readJson(detailResult).at("/data/activeSheetId").asText();
```

同一测试继续执行：

```java
PATCH /api/v2/sheets/{workbookId} with currentVersion=1, sheetId, edits[0,0]="42"
expected: status 200, currentVersion=2, grid[0][0]="42"
```

添加 `v21DocsSheetsShouldExposeExplicitUnsupportedAndPremiumBoundaries()`：

```java
GET /api/v2/docs/{noteId}/versions -> 403, code V2_ENTITLEMENT_REQUIRED
POST /api/v2/sheets/{workbookId}/imports with {"format":"CSV","content":"a,b\n1,2"} -> 400, code INVALID_ARGUMENT
POST /api/v2/sheets/{workbookId}/cleaning-rules -> 403, code V2_ENTITLEMENT_REQUIRED
GET /api/v2/sheets/{workbookId}/insights -> 403, code V2_ENTITLEMENT_REQUIRED
```

- [ ] **步骤 5：添加测试 helper**

添加这些 helper，并用 `/api/v2/docs`、`/api/v2/sheets` 创建测试数据：

```java
private String createV21Doc(String token, String title, String content) throws Exception
private String createV21Workbook(String token, String title) throws Exception
private String register(String email, String displayName) throws Exception
private JsonNode readJson(MvcResult result) throws Exception
```

helper payload 必须包含：

```json
{"title":"V21 Docs","content":"Initial body"}
{"title":"V21 Workbook","rowCount":4,"colCount":4}
{"email":"...","password":"Password@123","displayName":"..."}
```

- [ ] **步骤 6：运行红灯验证**

```bash
timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml test -Dtest=BackendV21DocsSheetsRuntimeBridgeTest -Dsurefire.failIfNoSpecifiedTests=false
```

预期：FAIL。失败应来自 `/api/v2/docs*` 或 `/api/v2/sheets*` 缺少 controller，不能是编译错误。

## 任务 2：实现 v2 Docs runtime bridge

**文件：**
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/controller/V21DocsController.java`
- 测试：`backend/mmmail-server/src/test/java/com/mmmail/server/BackendV21DocsSheetsRuntimeBridgeTest.java`

- [ ] **步骤 1：创建 `V21DocsController`**

类声明、依赖和构造函数：

```java
@RestController
@Validated
@RequestMapping("/api/v2/docs")
public class V21DocsController {

    private final DocsService docsService;
    private final DocsCollaborationService docsCollaborationService;

    public V21DocsController(DocsService docsService, DocsCollaborationService docsCollaborationService) {
        this.docsService = docsService;
        this.docsCollaborationService = docsCollaborationService;
    }
}
```

需要 imports：`Result`、Docs create/update/comment/share DTO、Docs summary/detail/comment/share VO、`JwtPrincipal`、`DocsService`、`DocsCollaborationService`、`SecurityUtils`、`HttpServletRequest`、`Valid`、Spring web annotations、`Validated`、`List`。

- [ ] **步骤 2：添加 Docs 文档路由**

添加这些方法：

```java
@GetMapping
public Result<List<DocsNoteSummaryVo>> list(
        @RequestParam(defaultValue = "") String keyword,
        @RequestParam(required = false) Integer limit
) {
    return Result.success(docsService.list(SecurityUtils.currentUserId(), keyword, limit));
}

@PostMapping
public Result<DocsNoteDetailVo> create(@Valid @RequestBody CreateDocsNoteRequest request, HttpServletRequest httpRequest) {
    JwtPrincipal principal = SecurityUtils.currentPrincipal();
    return Result.success(docsService.create(principal.userId(), principal.email(), principal.sessionId(),
            request.title(), request.content(), httpRequest.getRemoteAddr()));
}

@GetMapping("/{noteId}")
public Result<DocsNoteDetailVo> get(@PathVariable Long noteId) {
    return Result.success(docsService.get(SecurityUtils.currentUserId(), noteId));
}

@PatchMapping("/{noteId}")
public Result<DocsNoteDetailVo> update(
        @PathVariable Long noteId,
        @Valid @RequestBody UpdateDocsNoteRequest request,
        HttpServletRequest httpRequest
) {
    JwtPrincipal principal = SecurityUtils.currentPrincipal();
    return Result.success(docsService.update(principal.userId(), principal.email(), principal.sessionId(),
            noteId, request.title(), request.content(), request.currentVersion(), httpRequest.getRemoteAddr()));
}
```

- [ ] **步骤 3：添加 Docs comments 和 share 路由**

添加这些方法：

```java
@GetMapping("/{noteId}/comments")
public Result<List<DocsNoteCommentVo>> listComments(@PathVariable Long noteId) {
    return Result.success(docsCollaborationService.listComments(SecurityUtils.currentUserId(), noteId, true));
}

@PostMapping("/{noteId}/comments")
public Result<DocsNoteCommentVo> createComment(
        @PathVariable Long noteId,
        @Valid @RequestBody CreateDocsNoteCommentRequest request,
        HttpServletRequest httpRequest
) {
    JwtPrincipal principal = SecurityUtils.currentPrincipal();
    return Result.success(docsCollaborationService.createComment(principal.userId(), principal.email(),
            principal.sessionId(), noteId, request, httpRequest.getRemoteAddr()));
}

@PostMapping("/{noteId}/share")
public Result<DocsNoteShareVo> share(
        @PathVariable Long noteId,
        @Valid @RequestBody CreateDocsNoteShareRequest request,
        HttpServletRequest httpRequest
) {
    JwtPrincipal principal = SecurityUtils.currentPrincipal();
    return Result.success(docsCollaborationService.createShare(principal.userId(), principal.email(),
            principal.sessionId(), noteId, request, httpRequest.getRemoteAddr()));
}
```

- [ ] **步骤 4：运行 Docs 相关验证**

```bash
timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml test -Dtest=BackendV21DocsSheetsRuntimeBridgeTest,DocsCollaborationIntegrationTest,BackendV21AccessEntitlementGatesTest -Dsurefire.failIfNoSpecifiedTests=false
```

预期：Docs 路由断言通过；Sheets 路由断言仍因缺少 `V21SheetsController` 失败。

- [ ] **步骤 5：提交 Docs bridge**

```bash
git status --short --branch
git add backend/mmmail-server/src/main/java/com/mmmail/server/controller/V21DocsController.java backend/mmmail-server/src/test/java/com/mmmail/server/BackendV21DocsSheetsRuntimeBridgeTest.java
git diff --cached --check
git diff --cached --stat
git commit -m "feat(backend-v21): add docs runtime bridge"
```

## 任务 3：实现 v2 Sheets runtime bridge

**文件：**
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/controller/V21SheetsController.java`
- 测试：`backend/mmmail-server/src/test/java/com/mmmail/server/BackendV21DocsSheetsRuntimeBridgeTest.java`

- [ ] **步骤 1：创建 `V21SheetsController`**

类声明、依赖和构造函数：

```java
@RestController
@Validated
@RequestMapping("/api/v2/sheets")
public class V21SheetsController {

    private final SheetsService sheetsService;

    public V21SheetsController(SheetsService sheetsService) {
        this.sheetsService = sheetsService;
    }
}
```

需要 imports：`BizException`、`ErrorCode`、`Result`、`CreateSheetsWorkbookRequest`、`UpdateSheetsWorkbookCellsRequest`、Sheets detail/summary VO、`SheetsService`、`SecurityUtils`、`HttpServletRequest`、`Valid`、Spring web annotations、`Validated`、`List`。

- [ ] **步骤 2：添加 Sheets Community runtime 路由**

添加这些方法：

```java
@GetMapping
public Result<List<SheetsWorkbookSummaryVo>> list(@RequestParam(required = false) Integer limit) {
    return Result.success(sheetsService.list(SecurityUtils.currentUserId(), limit));
}

@PostMapping
public Result<SheetsWorkbookDetailVo> create(@Valid @RequestBody CreateSheetsWorkbookRequest request, HttpServletRequest httpRequest) {
    return Result.success(sheetsService.create(SecurityUtils.currentUserId(), request.title(),
            request.rowCount(), request.colCount(), httpRequest.getRemoteAddr()));
}

@GetMapping("/{workbookId}")
public Result<SheetsWorkbookDetailVo> get(@PathVariable Long workbookId) {
    return Result.success(sheetsService.get(SecurityUtils.currentUserId(), workbookId));
}

@PatchMapping("/{workbookId}")
public Result<SheetsWorkbookDetailVo> updateCells(
        @PathVariable Long workbookId,
        @Valid @RequestBody UpdateSheetsWorkbookCellsRequest request,
        HttpServletRequest httpRequest
) {
    return Result.success(sheetsService.updateCells(SecurityUtils.currentUserId(), workbookId,
            request.currentVersion(), request.sheetId(), request.edits(), httpRequest.getRemoteAddr()));
}
```

- [ ] **步骤 3：添加显式 Sheets JSON import 错误路由**

添加：

```java
@PostMapping("/{workbookId}/imports")
public Result<SheetsWorkbookDetailVo> importWorkbook(@PathVariable Long workbookId) {
    throw new BizException(
            ErrorCode.INVALID_ARGUMENT,
            "Sheets JSON import is not supported by the v2.1 runtime bridge"
    );
}
```

该方法不读取 request body，不调用 `SheetsService.importWorkbook()`，不返回导入结果。

- [ ] **步骤 4：运行 Sheets 相关验证**

```bash
timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml test -Dtest=BackendV21DocsSheetsRuntimeBridgeTest,SheetsWorkbookIntegrationTest,SheetsSharingVersionIntegrationTest,BackendV21AccessEntitlementGatesTest -Dsurefire.failIfNoSpecifiedTests=false
```

预期：PASS。

- [ ] **步骤 5：提交 Sheets bridge**

```bash
git status --short --branch
git add backend/mmmail-server/src/main/java/com/mmmail/server/controller/V21SheetsController.java
git diff --cached --check
git diff --cached --stat
git commit -m "feat(backend-v21): add sheets runtime bridge"
```

## 任务 4：进度更新和完整验证

**文件：**
- 修改：`docs/superpowers/progress/v21-implementation-progress.md`

- [ ] **步骤 1：执行完整后端验证**

```bash
timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml test -Dtest=BackendV21DocsSheetsRuntimeBridgeTest,DocsCollaborationIntegrationTest,SheetsWorkbookIntegrationTest,SheetsSharingVersionIntegrationTest,BackendV21AccessEntitlementGatesTest,BackendV21ApiContractCatalogTest -Dsurefire.failIfNoSpecifiedTests=false
```

预期：PASS。记录输出中的 Tests run、Failures、Errors、Skipped。

- [ ] **步骤 2：执行完整前端验证**

```bash
pnpm --dir frontend-v2 test
pnpm --dir frontend-v2 typecheck
pnpm --dir frontend-v2 build
```

预期：三个命令退出码均为 0。

- [ ] **步骤 3：更新 v2.1 进度**

先执行：

```bash
git log --oneline -3
```

用输出中的真实提交号更新 `docs/superpowers/progress/v21-implementation-progress.md`：

```markdown
| Backend Docs and Sheets runtime bridge (`backend-v21-docs-sheets-runtime-bridge`) | `BackendV21DocsSheetsRuntimeBridgeTest`, `V21DocsController`, `V21SheetsController` |
```

`Latest Completed Backend Slice` 必须写明本切片提交、变更摘要和刚运行的验证命令。`Active Backend Slice` 必须改为 `completed`，并保留本切片 scope。

- [ ] **步骤 4：提交进度**

```bash
git status --short --branch
git add -f docs/superpowers/progress/v21-implementation-progress.md
git diff --cached --check
git diff --cached --stat
git commit -m "docs(backend-v21): update docs sheets runtime bridge progress"
```

## 最终检查

- [ ] **步骤 1：确认工作区状态**

```bash
git status --short --branch
```

预期：无已跟踪未提交改动；既有未跟踪 `.superpowers/`、`.tmp/`、`docs/MMMail.zip`、`docs/MMMail/`、`frontend/` 不纳入提交。

- [ ] **步骤 2：确认提交历史**

```bash
git log --oneline -6
```

预期：包含 Docs bridge、Sheets bridge、进度更新提交。

## 计划自检

- 规格覆盖度：本计划覆盖设计中的 Docs Community bridge、Sheets Community bridge、Premium gate、unsupported import、测试和进度记录。
- 标记扫描：没有 unresolved marker 或未完成章节。
- 类型一致性：controller 使用现有 DTO/VO/service 名称；测试使用现有 `ErrorCode`、MockMvc 与 JSON 字段。
- 范围检查：计划不新增 Docs version persistence，不新增 Sheets import parser，不改前端视觉。
