# Backend v2.1 Collaboration Write Runtime 实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 为 v2.1 Community Collaboration 补齐项目、任务、评论写接口，并让读接口返回真实持久化数据、审计和 outbox 事件。

**架构：** 新增 Collaboration 专用轻量表和 `V21CollaborationWriteService`，由它拥有写侧事务、验证、审计和 outbox 发布。`V21OpsController` 只负责 v2 HTTP 入参与出参，`V21OpsRuntimeBridgeService` 继续负责 Ops 读模型聚合，并把持久化 Collaboration 数据排在审计派生数据之前。

**技术栈：** Java 21、Spring Boot、MockMvc、MyBatis-Plus、Flyway SQL migration、MySQL/H2 test profile、platform outbox。

---

## 文件结构

创建：

- `backend/mmmail-server/src/main/resources/db/migration/V13__v21_collaboration_write_runtime.sql`：新增 v2.1 Collaboration 项目、任务、评论表和索引。
- `backend/mmmail-server/src/main/java/com/mmmail/server/model/entity/V21CollaborationProject.java`：项目实体。
- `backend/mmmail-server/src/main/java/com/mmmail/server/model/entity/V21CollaborationTask.java`：任务实体。
- `backend/mmmail-server/src/main/java/com/mmmail/server/model/entity/V21CollaborationComment.java`：评论实体。
- `backend/mmmail-server/src/main/java/com/mmmail/server/mapper/V21CollaborationProjectMapper.java`：项目 mapper。
- `backend/mmmail-server/src/main/java/com/mmmail/server/mapper/V21CollaborationTaskMapper.java`：任务 mapper。
- `backend/mmmail-server/src/main/java/com/mmmail/server/mapper/V21CollaborationCommentMapper.java`：评论 mapper。
- `backend/mmmail-server/src/main/java/com/mmmail/server/model/dto/CreateV21CollaborationProjectRequest.java`：创建项目请求。
- `backend/mmmail-server/src/main/java/com/mmmail/server/model/dto/CreateV21CollaborationTaskRequest.java`：创建任务请求。
- `backend/mmmail-server/src/main/java/com/mmmail/server/model/dto/UpdateV21CollaborationTaskRequest.java`：更新任务请求。
- `backend/mmmail-server/src/main/java/com/mmmail/server/model/dto/CreateV21CollaborationCommentRequest.java`：创建评论请求。
- `backend/mmmail-server/src/main/java/com/mmmail/server/service/V21CollaborationWriteService.java`：写侧业务服务、读侧持久化合并、审计和 outbox 发布。
- `backend/mmmail-server/src/test/java/com/mmmail/server/BackendV21CollaborationWriteRuntimeTest.java`：本切片主集成测试。

修改：

- `backend/mmmail-server/src/main/resources/schema.sql`：同步新增表。
- `backend/mmmail-server/src/main/resources/db/baseline/community-v1-schema.sql`：同步新增表。
- `backend/mmmail-platform/src/main/java/com/mmmail/platform/event/PlatformEventType.java`：加入 Collaboration 写事件枚举。
- `backend/mmmail-server/src/test/java/com/mmmail/server/BackendV21EventOutboxFoundationTest.java`：加入新增事件目录断言。
- `backend/mmmail-server/src/main/java/com/mmmail/server/controller/V21OpsController.java`：把 Collaboration 写接口从 unsupported 改为真实请求处理。
- `backend/mmmail-server/src/main/java/com/mmmail/server/service/V21OpsRuntimeBridgeService.java`：注入写服务，合并持久化项目、任务、活动。
- `backend/mmmail-server/src/test/java/com/mmmail/server/BackendV21OpsRuntimeBridgeTest.java`：旧 unsupported 断言改成 Collaboration 写成功，同时保留 Notification/ Premium 失败边界。
- `docs/superpowers/progress/v21-implementation-progress.md`：实现完成后记录切片、提交和验证命令。

## 任务 1：写失败的 Collaboration 写接口集成测试

**文件：**

- 创建：`backend/mmmail-server/src/test/java/com/mmmail/server/BackendV21CollaborationWriteRuntimeTest.java`

- [ ] **步骤 1：创建失败测试类**

使用 MockMvc 写一个完整集成测试类，先覆盖项目创建、任务创建、任务更新、评论创建、读回、审计和 outbox。核心测试方法应包含这些断言：

```java
@Test
void v21CollaborationShouldPersistProjectTaskUpdateCommentAuditAndOutbox() throws Exception {
    String token = register("v21-collab-" + System.nanoTime() + "@mmmail.local");
    String projectId = createProject(token, "Launch Readiness", "WORKSPACE");
    String taskId = createTask(token, projectId, "Prepare acceptance checklist");

    mockMvc.perform(patch("/api/v2/collaboration/tasks/" + taskId)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "status": "DONE",
                              "assigneeEmail": "owner@example.com"
                            }
                            """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(taskId))
            .andExpect(jsonPath("$.data.status").value("DONE"));

    mockMvc.perform(post("/api/v2/collaboration/tasks/" + taskId + "/comments")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "body": "Acceptance checklist is ready"
                            }
                            """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.product").value("WORKSPACE"));

    mockMvc.perform(get("/api/v2/collaboration/projects").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].id").value(projectId))
            .andExpect(jsonPath("$.data[0].taskCount").value(1));

    mockMvc.perform(get("/api/v2/collaboration/tasks").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].id").value(taskId))
            .andExpect(jsonPath("$.data[0].status").value("DONE"));

    assertOutboxEvents();
    assertAuditEvents();
}
```

Add helper queries with `JdbcTemplate`:

```java
private void assertOutboxEvents() {
    Integer count = jdbcTemplate.queryForObject("""
            select count(*)
            from platform_outbox_event
            where event_type in (
              'collaboration.project.created.v1',
              'collaboration.task.created.v1',
              'collaboration.task.updated.v1',
              'collaboration.comment.created.v1'
            )
            """, Integer.class);
    assertThat(count).isEqualTo(4);
}
```

- [ ] **步骤 2：加入失败场景测试**

同一测试类加入：

```java
@Test
void v21CollaborationShouldRejectInvalidWriteInput() throws Exception {
    String token = register("v21-collab-invalid-" + System.nanoTime() + "@mmmail.local");

    mockMvc.perform(post("/api/v2/collaboration/projects")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"\"}"))
            .andExpect(status().isBadRequest());

    mockMvc.perform(post("/api/v2/collaboration/tasks")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "projectId": "999999",
                              "title": "Missing project"
                            }
                            """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_ARGUMENT.getCode()));
}
```

- [ ] **步骤 3：运行测试确认失败**

运行：

```bash
timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml test -Dtest=BackendV21CollaborationWriteRuntimeTest -Dsurefire.failIfNoSpecifiedTests=false
```

预期：FAIL，失败点是 `POST /api/v2/collaboration/projects` 仍返回 `INVALID_ARGUMENT` unsupported，或测试类引用的新行为不存在。

- [ ] **步骤 4：Commit 测试红灯**

```bash
git add backend/mmmail-server/src/test/java/com/mmmail/server/BackendV21CollaborationWriteRuntimeTest.java
git commit -m "test(backend-v21): cover collaboration write runtime"
```

## 任务 2：新增数据表、实体、mapper 和事件枚举

**文件：**

- 创建：任务 1 文件结构中列出的 migration、entity、mapper 文件。
- 修改：`backend/mmmail-server/src/main/resources/schema.sql`
- 修改：`backend/mmmail-server/src/main/resources/db/baseline/community-v1-schema.sql`
- 修改：`backend/mmmail-platform/src/main/java/com/mmmail/platform/event/PlatformEventType.java`
- 修改：`backend/mmmail-server/src/test/java/com/mmmail/server/BackendV21EventOutboxFoundationTest.java`

- [ ] **步骤 1：添加 migration**

`V13__v21_collaboration_write_runtime.sql` 使用以下表和索引形态：

```sql
create table if not exists v21_collaboration_project (
    id bigint primary key,
    owner_id bigint not null,
    name varchar(160) not null,
    product varchar(32) not null,
    status varchar(32) not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted tinyint not null default 0
);

create unique index uk_v21_collab_project_owner_name
    on v21_collaboration_project(owner_id, name, deleted);
create index idx_v21_collab_project_owner_updated
    on v21_collaboration_project(owner_id, updated_at);
create index idx_v21_collab_project_owner_product_status
    on v21_collaboration_project(owner_id, product, status);

create table if not exists v21_collaboration_task (
    id bigint primary key,
    project_id bigint not null,
    owner_id bigint not null,
    title varchar(220) not null,
    product varchar(32) not null,
    status varchar(32) not null,
    assignee_email varchar(190),
    due_at timestamp,
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted tinyint not null default 0
);

create index idx_v21_collab_task_owner_updated
    on v21_collaboration_task(owner_id, updated_at);
create index idx_v21_collab_task_project_status_updated
    on v21_collaboration_task(project_id, status, updated_at);
create index idx_v21_collab_task_owner_status_due
    on v21_collaboration_task(owner_id, status, due_at);

create table if not exists v21_collaboration_comment (
    id bigint primary key,
    task_id bigint not null,
    project_id bigint not null,
    owner_id bigint not null,
    author_user_id bigint not null,
    body text not null,
    created_at timestamp not null,
    deleted tinyint not null default 0
);

create index idx_v21_collab_comment_task_created
    on v21_collaboration_comment(task_id, created_at);
create index idx_v21_collab_comment_owner_created
    on v21_collaboration_comment(owner_id, created_at);

update system_release_metadata
set schema_version = '13',
    updated_at = current_timestamp
where id = 1;
```

- [ ] **步骤 2：添加实体和 mapper**

Entities follow the existing mutable MyBatis-Plus pattern:

```java
@TableName("v21_collaboration_project")
public class V21CollaborationProject {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long ownerId;
    private String name;
    private String product;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic(value = "0", delval = "1")
    private Integer deleted;
    // getters and setters for every field
}
```

Each mapper is a direct `BaseMapper<T>` with `@Mapper`.

- [ ] **步骤 3：扩展平台事件枚举**

Add enum values:

```java
COLLABORATION_PROJECT_CREATED("collaboration.project.created.v1", "collaboration", true, true, true),
COLLABORATION_TASK_CREATED("collaboration.task.created.v1", "collaboration", true, true, true),
COLLABORATION_TASK_UPDATED_V1("collaboration.task.updated.v1", "collaboration", true, true, true),
COLLABORATION_COMMENT_CREATED("collaboration.comment.created.v1", "collaboration", true, true, true),
```

Keep existing `COLLABORATION_TASK_UPDATED("collaboration.task.updated", ...)` for compatibility with current tests.

- [ ] **步骤 4：更新 outbox 目录测试**

Add the four `.v1` event names to `BackendV21EventOutboxFoundationTest.REQUIRED_EVENTS`.

- [ ] **步骤 5：运行结构验证**

运行：

```bash
timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml test -Dtest=BackendV21EventOutboxFoundationTest -Dsurefire.failIfNoSpecifiedTests=false
```

预期：PASS。

- [ ] **步骤 6：Commit 数据模型**

```bash
git add backend/mmmail-server/src/main/resources/db/migration/V13__v21_collaboration_write_runtime.sql backend/mmmail-server/src/main/resources/schema.sql backend/mmmail-server/src/main/resources/db/baseline/community-v1-schema.sql backend/mmmail-server/src/main/java/com/mmmail/server/model/entity/V21CollaborationProject.java backend/mmmail-server/src/main/java/com/mmmail/server/model/entity/V21CollaborationTask.java backend/mmmail-server/src/main/java/com/mmmail/server/model/entity/V21CollaborationComment.java backend/mmmail-server/src/main/java/com/mmmail/server/mapper/V21CollaborationProjectMapper.java backend/mmmail-server/src/main/java/com/mmmail/server/mapper/V21CollaborationTaskMapper.java backend/mmmail-server/src/main/java/com/mmmail/server/mapper/V21CollaborationCommentMapper.java backend/mmmail-platform/src/main/java/com/mmmail/platform/event/PlatformEventType.java backend/mmmail-server/src/test/java/com/mmmail/server/BackendV21EventOutboxFoundationTest.java
git commit -m "feat(backend-v21): add collaboration write persistence"
```

## 任务 3：实现 DTO 和 `V21CollaborationWriteService`

**文件：**

- 创建：四个 DTO 文件。
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/service/V21CollaborationWriteService.java`

- [ ] **步骤 1：添加 DTO**

Use records with validation annotations:

```java
public record CreateV21CollaborationProjectRequest(
        @NotBlank @Size(max = 160) String name,
        @Size(max = 32) String product,
        @Size(max = 32) String status
) {
}
```

`CreateV21CollaborationTaskRequest` has `@NotBlank String projectId`, `@NotBlank @Size(max = 220) String title`, `String status`, `String assigneeEmail`, and `LocalDateTime dueAt`.

`UpdateV21CollaborationTaskRequest` has nullable `projectId`, `title`, `status`, `assigneeEmail`, and `dueAt`.

`CreateV21CollaborationCommentRequest` has `@NotBlank @Size(max = 4000) String body`.

- [ ] **步骤 2：实现写服务公共 API**

`V21CollaborationWriteService` must expose these methods:

```java
public V21CollaborationProjectVo createProject(Long userId, CreateV21CollaborationProjectRequest request, String ipAddress)
public V21CollaborationTaskVo createTask(Long userId, CreateV21CollaborationTaskRequest request, String ipAddress)
public V21CollaborationTaskVo updateTask(Long userId, String taskId, UpdateV21CollaborationTaskRequest request, String ipAddress)
public V21CollaborationActivityVo createComment(Long userId, String taskId, CreateV21CollaborationCommentRequest request, String ipAddress)
public List<V21CollaborationProjectVo> listPersistedProjects(Long userId, Integer limit)
public V21CollaborationProjectVo readPersistedProject(Long userId, String projectId)
public List<V21CollaborationTaskVo> listPersistedTasks(Long userId, Integer limit)
public List<V21CollaborationActivityVo> listPersistedActivity(Long userId, Integer limit)
```

- [ ] **步骤 3：实现规则**

Use constants for statuses and defaults:

```java
private static final String DEFAULT_PRODUCT = "WORKSPACE";
private static final String PROJECT_STATUS_ACTIVE = "ACTIVE";
private static final Set<String> TASK_STATUSES = Set.of("OPEN", "IN_PROGRESS", "BLOCKED", "DONE", "ARCHIVED");
```

Normalize product and status with trim + uppercase. Parse string IDs with a helper that throws `BizException(ErrorCode.INVALID_ARGUMENT, "... id is invalid")`.

- [ ] **步骤 4：实现审计和 outbox**

For each write, call `auditService.record(...)` and `outboxPublisher.publish(...)`. Build metadata with:

```java
new PlatformEventMetadata(
        "community",
        String.valueOf(userId),
        null,
        null,
        "collaboration",
        operation,
        now
)
```

Use aggregate types `collaboration_project`, `collaboration_task`, and `collaboration_comment`. Use idempotency keys such as `v21-collaboration-project-created-<id>`.

- [ ] **步骤 5：运行任务 1 测试**

```bash
timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml test -Dtest=BackendV21CollaborationWriteRuntimeTest -Dsurefire.failIfNoSpecifiedTests=false
```

预期：此时仍可能 FAIL，因为 controller 和 bridge 还未调用服务。

## 任务 4：接入 v2 Ops controller 和 read bridge

**文件：**

- 修改：`backend/mmmail-server/src/main/java/com/mmmail/server/controller/V21OpsController.java`
- 修改：`backend/mmmail-server/src/main/java/com/mmmail/server/service/V21OpsRuntimeBridgeService.java`

- [ ] **步骤 1：控制器接入写接口**

Replace Collaboration unsupported methods with typed requests:

```java
@PostMapping("/collaboration/projects")
public Result<V21CollaborationProjectVo> createProject(
        @Valid @RequestBody CreateV21CollaborationProjectRequest request,
        HttpServletRequest httpRequest
) {
    return Result.success(opsRuntimeBridgeService.createProject(SecurityUtils.currentUserId(), request, httpRequest));
}
```

Apply the same pattern for task create, task patch, and task comment.

- [ ] **步骤 2：bridge 注入写服务**

Add constructor dependency `V21CollaborationWriteService collaborationWriteService`. Add pass-through write methods and update read methods so persisted rows come first:

```java
public List<V21CollaborationTaskVo> listTasks(Long userId, Integer limit, HttpServletRequest request) {
    List<V21CollaborationTaskVo> persisted = collaborationWriteService.listPersistedTasks(userId, limit);
    List<V21CollaborationTaskVo> derived = collaborationCenter(userId, limit, request).items().stream()
            .map(this::toTask)
            .toList();
    return mergeTasks(persisted, derived, limit);
}
```

Deduplicate by `id`. Keep existing event-derived rows as read-only supplements.

- [ ] **步骤 3：运行主测试**

```bash
timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml test -Dtest=BackendV21CollaborationWriteRuntimeTest -Dsurefire.failIfNoSpecifiedTests=false
```

预期：PASS。

- [ ] **步骤 4：Commit 运行时接入**

```bash
git add backend/mmmail-server/src/main/java/com/mmmail/server/model/dto/CreateV21CollaborationProjectRequest.java backend/mmmail-server/src/main/java/com/mmmail/server/model/dto/CreateV21CollaborationTaskRequest.java backend/mmmail-server/src/main/java/com/mmmail/server/model/dto/UpdateV21CollaborationTaskRequest.java backend/mmmail-server/src/main/java/com/mmmail/server/model/dto/CreateV21CollaborationCommentRequest.java backend/mmmail-server/src/main/java/com/mmmail/server/service/V21CollaborationWriteService.java backend/mmmail-server/src/main/java/com/mmmail/server/controller/V21OpsController.java backend/mmmail-server/src/main/java/com/mmmail/server/service/V21OpsRuntimeBridgeService.java
git commit -m "feat(backend-v21): bridge collaboration writes"
```

## 任务 5：更新 Ops 回归测试并保留边界

**文件：**

- 修改：`backend/mmmail-server/src/test/java/com/mmmail/server/BackendV21OpsRuntimeBridgeTest.java`

- [ ] **步骤 1：替换旧 unsupported 断言**

Rename `v21OpsShouldRejectUnsupportedCommunityWritesAndGatePremiumRoutes` to `v21OpsShouldSupportCollaborationWritesAndKeepOtherOpsBoundaries`。

Use real Collaboration calls:

```java
String projectId = createV21Project(token, "Ops Bridge Project");
String taskId = createV21Task(token, projectId, "Ops bridge task");
patchV21Task(token, taskId);
commentV21Task(token, taskId);
```

Keep:

```java
assertUnsupportedPatch(token, "/api/v2/notifications/subscriptions/web-push-mail-inbox");
assertPremiumPostGate(token, "/api/v2/command-center/runs");
assertPremiumGate(token, "/api/v2/notifications/rules");
assertPremiumPostGate(token, "/api/v2/notifications/send");
```

- [ ] **步骤 2：运行 Ops 回归**

```bash
timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml test -Dtest=BackendV21OpsRuntimeBridgeTest -Dsurefire.failIfNoSpecifiedTests=false
```

预期：PASS。

- [ ] **步骤 3：Commit 测试调整**

```bash
git add backend/mmmail-server/src/test/java/com/mmmail/server/BackendV21OpsRuntimeBridgeTest.java
git commit -m "test(backend-v21): update ops collaboration boundary"
```

## 任务 6：完整验证、进度记录和收尾提交

**文件：**

- 修改：`docs/superpowers/progress/v21-implementation-progress.md`

- [ ] **步骤 1：运行后端切片验证**

```bash
timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml test -Dtest=BackendV21CollaborationWriteRuntimeTest,BackendV21OpsRuntimeBridgeTest -Dsurefire.failIfNoSpecifiedTests=false
```

预期：PASS。

- [ ] **步骤 2：运行契约和门禁回归**

```bash
timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml test -Dtest=BackendV21AccessEntitlementGatesTest,BackendV21ApiContractCatalogTest,BackendV21EventOutboxFoundationTest -Dsurefire.failIfNoSpecifiedTests=false
```

预期：PASS。

- [ ] **步骤 3：前端轻量回归**

只有当 API response shape 变化影响 `frontend-v2` client 时运行完整前端；本切片默认至少运行：

```bash
pnpm --dir frontend-v2 test
pnpm --dir frontend-v2 typecheck
```

预期：PASS。

- [ ] **步骤 4：更新进度文件**

在 `Completed v2.1 Slices` 增加：

```markdown
| Backend Collaboration write runtime (`backend-v21-collaboration-write-runtime`) | `BackendV21CollaborationWriteRuntimeTest`, `BackendV21OpsRuntimeBridgeTest`, `V21CollaborationWriteService`, `v21_collaboration_project/task/comment` |
```

在 `Latest Completed Backend Slice` 改为本切片，记录最终提交 hash 和验证命令。

- [ ] **步骤 5：Commit 进度记录**

```bash
git add docs/superpowers/progress/v21-implementation-progress.md
git commit -m "docs(backend-v21): update collaboration write runtime progress"
```

- [ ] **步骤 6：最终状态检查**

```bash
git status --short --branch
git log --oneline -5
```

预期：只剩既有无关未跟踪路径，最新提交包含本切片进度记录。

## 自检

- 规格覆盖度：计划覆盖项目创建、任务创建、任务更新、评论创建、读路由合并、审计、outbox、migration、baseline schema、测试和进度记录。
- 占位符扫描：计划没有未决项；每个任务都有具体文件、命令和预期结果。
- 类型一致性：DTO、service、controller、VO、event type 名称在任务间保持一致。
- 范围控制：不实现 Premium automation、Command Center run/workflow、Notification rules/templates/send/analytics。
