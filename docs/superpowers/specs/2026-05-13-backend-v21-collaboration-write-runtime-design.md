# Backend v2.1 Collaboration Write Runtime Design

## 1. Purpose

This slice closes the Community Collaboration write gap in v2.1. The current Ops runtime bridge exposes read models for Collaboration by deriving projects, tasks, and activity from existing Suite audit events, but the Community write endpoints intentionally fail with explicit unsupported errors. The v2.1 master specification requires Community Collaboration to support projects, tasks, comments, and activity, so this slice adds real persistence and runtime behavior for those write endpoints.

The implementation must keep the existing no-silent-fallback policy. Unsupported Premium automation, AI summaries, organization analytics, Command Center execution, and advanced Notification rules remain explicit entitlement or unsupported boundaries.

## 2. Current Context

Completed backend v2.1 slices already provide:

- API contract catalog and `/api/v2/platform/contracts`.
- Access entitlement gates through `V21ApiAccessGateInterceptor`.
- Platform outbox event persistence.
- Platform background job persistence.
- Runtime bridges for Calendar, Drive, Docs, Sheets, Mail, Pass, and Ops.
- Current Ops bridge read routes for Collaboration, Notifications, and Command Center.

The current unsupported Community Collaboration routes are:

```text
POST /api/v2/collaboration/projects
POST /api/v2/collaboration/tasks
PATCH /api/v2/collaboration/tasks/{id}
POST /api/v2/collaboration/tasks/{id}/comments
```

## 3. Selected Approach

Use a dedicated lightweight Collaboration persistence model:

- `v21_collaboration_project`
- `v21_collaboration_task`
- `v21_collaboration_comment`

This is preferred over reusing `lumo_project` because Lumo is an AI/Labs concept, not the owner of general team projects and task discussions. It is also preferred over audit-only writes because audit events cannot reliably own task status, assignment, due dates, comments, or project detail state.

## 4. Scope

This slice includes:

- Project creation.
- Task creation.
- Task status, title, assignee, due-date, and project movement updates.
- Task comment creation.
- Read-route integration so `GET /projects`, `GET /projects/{id}`, `GET /tasks`, and `GET /activity` include real Collaboration records.
- Audit records for all writes.
- Outbox events for write-side integration.
- Backend integration tests covering the v2 API surface.

This slice excludes:

- Premium approvals, automation, AI summaries, and organization collaboration analytics.
- Command Center run/workflow/audit execution.
- Notification rules, templates, multi-channel sending, delivery analytics, and webhooks.
- WebSocket/SSE task live updates beyond the existing Collaboration stream model.
- Frontend redesign or new visual work.

## 5. API Behavior

### 5.1 Project Creation

`POST /api/v2/collaboration/projects` creates a Community project owned by the current user.

Required request fields:

- `name`

Optional request fields:

- `product`
- `status`

Default behavior:

- Empty product defaults to `WORKSPACE`.
- Empty status defaults to `ACTIVE`.
- The response returns `V21CollaborationProjectVo`.

Validation:

- Blank names fail with `INVALID_ARGUMENT`.
- Duplicate undeleted project names for the same owner fail visibly.
- Unsupported status values fail visibly.

### 5.2 Task Creation

`POST /api/v2/collaboration/tasks` creates a task in an existing Collaboration project.

Required request fields:

- `projectId`
- `title`

Optional request fields:

- `status`
- `assigneeEmail`
- `dueAt`

Default behavior:

- Empty status defaults to `OPEN`.
- The response returns `V21CollaborationTaskVo`.

Validation:

- Unknown project IDs fail with `INVALID_ARGUMENT`.
- Blank titles fail with `INVALID_ARGUMENT`.
- Unsupported status values fail visibly.

### 5.3 Task Update

`PATCH /api/v2/collaboration/tasks/{id}` updates mutable task fields.

Mutable fields:

- `projectId`
- `title`
- `status`
- `assigneeEmail`
- `dueAt`

Rules:

- At least one mutable field must be present.
- Unknown task IDs fail with `INVALID_ARGUMENT`.
- Unknown project IDs fail with `INVALID_ARGUMENT`.
- Status transitions are intentionally simple for Community: `OPEN`, `IN_PROGRESS`, `BLOCKED`, `DONE`, and `ARCHIVED`.

The response returns `V21CollaborationTaskVo`.

### 5.4 Comment Creation

`POST /api/v2/collaboration/tasks/{id}/comments` creates a task comment.

Required request fields:

- `body`

Rules:

- Unknown task IDs fail with `INVALID_ARGUMENT`.
- Blank comment bodies fail with `INVALID_ARGUMENT`.
- The response returns a v2 Collaboration activity row so callers can append it without refetching the whole activity feed.

## 6. Data Model

### 6.1 `v21_collaboration_project`

Owned by the Collaboration module.

Columns:

- `id` bigint primary key.
- `owner_id` bigint not null.
- `name` varchar(160) not null.
- `product` varchar(32) not null.
- `status` varchar(32) not null.
- `created_at` datetime not null.
- `updated_at` datetime not null.
- `deleted` tinyint not null default 0.

Indexes:

- Unique owner/name/deleted index.
- Owner/updated index.
- Owner/product/status index.

### 6.2 `v21_collaboration_task`

Owned by the Collaboration module.

Columns:

- `id` bigint primary key.
- `project_id` bigint not null.
- `owner_id` bigint not null.
- `title` varchar(220) not null.
- `product` varchar(32) not null.
- `status` varchar(32) not null.
- `assignee_email` varchar(190).
- `due_at` datetime.
- `created_at` datetime not null.
- `updated_at` datetime not null.
- `deleted` tinyint not null default 0.

Indexes:

- Owner/updated index.
- Project/status/updated index.
- Owner/status/due index.

### 6.3 `v21_collaboration_comment`

Owned by the Collaboration module.

Columns:

- `id` bigint primary key.
- `task_id` bigint not null.
- `project_id` bigint not null.
- `owner_id` bigint not null.
- `author_user_id` bigint not null.
- `body` text not null.
- `created_at` datetime not null.
- `deleted` tinyint not null default 0.

Indexes:

- Task/created index.
- Owner/created index.

## 7. Service Boundaries

### 7.1 New Service

Add `V21CollaborationWriteService`.

Responsibilities:

- Validate request payloads.
- Own project/task/comment write transactions.
- Load readable project and task records for the current user.
- Convert entities into v2 VO records.
- Record audit events.
- Publish outbox events.

### 7.2 Existing Ops Bridge

`V21OpsRuntimeBridgeService` remains the v2 read aggregation bridge for Ops pages. It should delegate real Collaboration writes to `V21CollaborationWriteService` and include persisted Collaboration records in read responses.

It must not duplicate validation rules, transaction logic, mapper access, audit writing, or outbox publishing.

### 7.3 Controller Boundary

`V21OpsController` should:

- Accept typed request DTOs.
- Resolve current user and request context.
- Return typed v2 VO responses.
- Keep business rules in services.

## 8. Read Model Integration

`GET /api/v2/collaboration/projects` should return persisted projects first, ordered by `updatedAt` descending. Derived cross-product projects from existing audit events can remain as supplemental rows when no persisted project has the same product-derived ID.

`GET /api/v2/collaboration/projects/{id}` should first read a persisted project by ID. If no persisted project exists, it can keep the existing derived-project behavior.

`GET /api/v2/collaboration/tasks` should return persisted tasks first, ordered by `updatedAt` descending, then keep existing event-derived tasks as supplemental read-only rows.

`GET /api/v2/collaboration/activity` should include:

- Project creation activity.
- Task creation activity.
- Task update activity.
- Comment creation activity.
- Existing cross-module audit-derived activity.

Activity ordering is descending by occurrence time unless an existing endpoint contract already requires a different order.

## 9. Audit and Outbox

Every write must record an audit event with enough detail to trace the resource and actor:

- `V21_COLLABORATION_PROJECT_CREATE`
- `V21_COLLABORATION_TASK_CREATE`
- `V21_COLLABORATION_TASK_UPDATE`
- `V21_COLLABORATION_COMMENT_CREATE`

Every write must also enqueue a platform outbox event:

- `collaboration.project.created.v1`
- `collaboration.task.created.v1`
- `collaboration.task.updated.v1`
- `collaboration.comment.created.v1`

Outbox payloads should include resource IDs, owner ID, product, status where relevant, and timestamps. They must not include comment body content beyond what is necessary for internal display, because comments may contain sensitive collaboration text.

## 10. Error Handling

The slice follows the repository debug-first policy:

- No mock success responses.
- No silent degradation.
- No defensive catch-and-ignore blocks.
- Invalid input fails with `INVALID_ARGUMENT`.
- Entitlement-gated Premium endpoints continue to fail through the existing access gate.
- Missing owned resources fail visibly instead of returning null success payloads.

## 11. Tests

Add `BackendV21CollaborationWriteRuntimeTest` covering:

- Project creation and read-back.
- Duplicate undeleted project name rejection.
- Task creation and read-back.
- Task update and read-back.
- Task comment creation and activity feed evidence.
- Invalid project/task/comment input failures.
- Outbox records for project, task, update, and comment writes.
- Audit records for project, task, update, and comment writes.

Extend or preserve `BackendV21OpsRuntimeBridgeTest` so previously unsupported Community writes now assert real success for Collaboration only, while notification subscription mutation and Premium Ops endpoints remain explicit failures.

Regression verification:

```bash
timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml test -Dtest=BackendV21CollaborationWriteRuntimeTest,BackendV21OpsRuntimeBridgeTest -Dsurefire.failIfNoSpecifiedTests=false
timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml test -Dtest=BackendV21AccessEntitlementGatesTest,BackendV21ApiContractCatalogTest -Dsurefire.failIfNoSpecifiedTests=false
```

Frontend regression is not required for this backend-only slice unless v2 API response shapes change in a way that affects `frontend-v2` clients.

## 12. Acceptance Criteria

- Community Collaboration project, task, task update, and comment write endpoints return real persisted results.
- Collaboration read routes expose persisted records and keep useful existing cross-module activity.
- Notification subscription mutation remains unsupported unless a real service target is introduced in a later slice.
- Premium Command Center and Notification routes remain gated by entitlement behavior.
- New persistence is represented in Flyway migration and baseline schema.
- Backend tests prove write behavior, validation failures, audit records, outbox records, and entitlement regressions.

## 13. Specification Self-Check

- Placeholder scan: no placeholder sections or unresolved requirements remain.
- Internal consistency: API behavior, data model, service boundaries, audit, and tests all describe the same slice.
- Scope check: this is one backend runtime slice focused on Community Collaboration writes; Premium Ops and Notification advanced capabilities are explicitly excluded.
- Ambiguity check: derived read rows remain supplemental only, while persisted Collaboration records own all new write-side state.
