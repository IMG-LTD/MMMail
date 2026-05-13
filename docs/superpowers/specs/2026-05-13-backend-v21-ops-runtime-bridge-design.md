# Backend v2.1 Ops Runtime Bridge Design

Date: 2026-05-13

## Context

The previous backend v2.1 slices completed API contract publication, event outbox foundation, background jobs, access entitlement gates, and runtime bridges for Calendar, Drive, Docs, Sheets, Mail, and Pass.

The next backend runtime gap is the operations surface used by frontend v2.1:

- Collaboration
- Notifications
- Command Center

Frontend v2.1 already calls `/api/v2/collaboration/*`, `/api/v2/notifications/*`, and `/api/v2/command-center/*`. Backend v1 already has real runtime behavior through `SuiteCollaborationService`, `SuiteCommandCenterService`, `SuiteNotificationSyncService`, and `WebPushService`.

## Decision

Implement one focused slice: `backend-v21-ops-runtime-bridge`.

The slice adds a thin v2 operations bridge that maps the v2.1 operations API contracts to existing real Suite services. It must not add mock data, fake success responses, silent v1 URL fallback, or new entitlement bypasses.

## Selected Approach

Use a thin v2 adapter plus explicit v2 response mapping.

Rejected alternatives:

- Build new Collaboration, Notifications, and Command Center domain models first. This is cleaner long term but too large for the current runtime bridge slice.
- Return existing v1 Suite VO models directly. This is fast but leaks v1 aggregation shapes into frozen v2 frontend API clients.

## Scope

In scope:

- Add one v2 operations controller for Collaboration, Notifications, and Command Center routes.
- Add one focused runtime bridge service that delegates to existing Suite services and maps responses to v2 API shapes.
- Add small v2 request and response records only when existing v1 DTOs or VOs do not match the v2 frontend contract.
- Cover Community runtime reads and supported state changes with backend integration tests.
- Keep Premium operations endpoints behind the existing v2 entitlement gate.
- Update v2.1 progress after implementation.

Out of scope:

- New collaboration project or task persistence tables.
- New command run, workflow, audit, or log persistence.
- Notification delivery engine redesign.
- Web Push configuration changes.
- Admin, Billing, Settings, Public Share, Workspace summary, AI, or MCP runtime bridges.
- Broad frontend UI redesign.

## Architecture

The v2 operations bridge is an API compatibility adapter:

- It owns the `/api/v2/collaboration/*`, `/api/v2/notifications/*`, and `/api/v2/command-center/*` route surface for this slice.
- It resolves the authenticated user and session through existing security utilities.
- It resolves organization scope through existing Suite scope support where the delegated service already accepts scope filtering.
- It delegates real behavior to existing Suite services.
- It maps Suite VO payloads to v2 frontend-facing records.
- It relies on the existing v2 access gate for authentication, entitlement, permission, and unknown-contract handling.

Business behavior remains in existing services. The bridge must not duplicate Suite aggregation rules, notification state rules, audit behavior, scope filtering, Web Push state, command center quick-route construction, or collaboration event aggregation.

## Collaboration Route Mapping

| v2 route | Runtime mapping | Entitlement |
| --- | --- | --- |
| `GET /api/v2/collaboration/projects` | Derive v2 projects from real `SuiteCollaborationService.getCenter(...)` product counts and visible activity | Community |
| `POST /api/v2/collaboration/projects` | Explicit unsupported operation unless a real existing service target is added in the implementation plan | Community |
| `GET /api/v2/collaboration/projects/{id}` | Read derived project by id from the same real collaboration center snapshot | Community |
| `GET /api/v2/collaboration/tasks` | Derive v2 task rows from real collaboration events that imply user-actionable work | Community |
| `POST /api/v2/collaboration/tasks` | Explicit unsupported operation unless a real existing service target is added in the implementation plan | Community |
| `PATCH /api/v2/collaboration/tasks/{id}` | Explicit unsupported operation unless a real existing service target is added in the implementation plan | Community |
| `POST /api/v2/collaboration/tasks/{id}/comments` | Explicit unsupported operation unless a real existing service target is added in the implementation plan | Community |
| `GET /api/v2/collaboration/activity` | Map `SuiteCollaborationCenterVo.items` to v2 collaboration activity rows | Community |

Derived project and task rows are allowed only when they are traceable to real persisted audit or collaboration state. The bridge must not invent static project names or fake task completion state.

## Notifications Route Mapping

| v2 route | Runtime mapping | Entitlement |
| --- | --- | --- |
| `GET /api/v2/notifications` | Map `SuiteCommandCenterService.getNotificationCenter(...)` items to v2 notification rows | Community |
| `PATCH /api/v2/notifications/{id}` | Support real read-state update by delegating to `markNotificationsRead(...)` when body requests `status=READ` | Community |
| `GET /api/v2/notifications/subscriptions` | Map `WebPushService.getStatus(...)` to a v2 subscription summary | Community |
| `PATCH /api/v2/notifications/subscriptions/{id}` | Explicit unsupported operation unless a real existing subscription mutation target is present | Community |
| `GET /api/v2/notifications/rules` | Blocked before controller by Premium gate | Premium |
| `POST /api/v2/notifications/rules` | Blocked before controller by Premium gate | Premium |
| `GET /api/v2/notifications/templates` | Blocked before controller by Premium gate | Premium |
| `POST /api/v2/notifications/send` | Blocked before controller by Premium gate | Premium |
| `GET /api/v2/notifications/analytics` | Blocked before controller by Premium gate | Premium |

`PATCH /api/v2/notifications/{id}` is intentionally narrow. It supports marking a notification read because the existing Suite notification state model supports that operation. Other body values return an explicit invalid-argument error.

## Command Center Route Mapping

| v2 route | Runtime mapping | Entitlement |
| --- | --- | --- |
| `GET /api/v2/command-center/commands` | Map `SuiteCommandCenterService.getCommandCenter(...)` quick routes and recommended actions to command templates | Community |
| `GET /api/v2/command-center/commands/{id}` | Read one command from the same real command set | Community |
| `POST /api/v2/command-center/runs` | Blocked before controller by Premium gate | Premium |
| `GET /api/v2/command-center/runs/{id}` | Blocked before controller by Premium gate | Premium |
| `POST /api/v2/command-center/runs/{id}/cancel` | Blocked before controller by Premium gate | Premium |
| `POST /api/v2/command-center/runs/{id}/retry` | Blocked before controller by Premium gate | Premium |
| `GET /api/v2/command-center/workflows` | Blocked before controller by Premium gate | Premium |
| `POST /api/v2/command-center/workflows` | Blocked before controller by Premium gate | Premium |
| `GET /api/v2/command-center/audit` | Blocked before controller by Premium gate | Premium |

Community command templates are read-only. Executing commands, workflow authoring, command run lifecycle, and audit export remain Premium boundaries in this slice.

## V2 Adapter Shapes

Add only focused v2 adapter records:

- `V21CollaborationProjectVo`: `id`, `name`, `status`, `taskCount`, `updatedAt`.
- `V21CollaborationTaskVo`: `id`, `projectId`, `title`, `status`, `assigneeEmail`, `dueAt`.
- `V21CollaborationActivityVo`: `id`, `title`, `product`, `occurredAt`.
- `V21NotificationVo`: `id`, `title`, `body`, `product`, `severity`, `status`, `createdAt`, `readAt`.
- `V21NotificationSubscriptionVo`: `id`, `product`, `channel`, `enabled`.
- `V21NotificationPatchRequest`: `status`.
- `V21CommandCenterCommandVo`: `id`, `name`, `description`, `product`, `enabled`, `parameterCount`.

The implementation may keep these records in one cohesive model file only if it stays below repository file-size limits. Otherwise they should be split by route family.

## Error Handling

No silent fallback is introduced.

- Missing or invalid authentication is handled by existing security infrastructure.
- Premium routes continue to return `V2_ENTITLEMENT_REQUIRED` for Community users through `V21ApiAccessGateInterceptor`.
- Unsupported Community write routes return an explicit invalid-argument or unsupported-operation error; they must not return empty success payloads.
- Unknown ids return explicit not-found or invalid-argument errors.
- Invalid notification patch bodies return explicit invalid-argument errors.
- Existing service-level scope, permission, and validation errors remain the source of truth.

## Tests

Add `BackendV21OpsRuntimeBridgeTest` before implementation as a red test.

Required coverage:

- Register or log in a real user.
- Seed real Suite activity through existing Mail, Calendar, Drive, Pass, governance, or notification-producing services where practical.
- Call `GET /api/v2/collaboration/activity` and assert rows come from real Suite collaboration events.
- Call `GET /api/v2/collaboration/projects` and `GET /api/v2/collaboration/tasks` and assert rows are derived from real runtime state.
- Call `GET /api/v2/notifications` and assert notification rows reflect real Suite notification center output.
- Call `PATCH /api/v2/notifications/{id}` with `status=READ` and assert the notification becomes read.
- Call `GET /api/v2/notifications/subscriptions` and assert Web Push status is mapped to a v2 subscription record.
- Call `GET /api/v2/command-center/commands` and `GET /api/v2/command-center/commands/{id}` and assert command templates come from real command center output.
- Verify Premium operations routes return `V2_ENTITLEMENT_REQUIRED` for Community users, including command runs, workflows, audit, notification rules, templates, send, and analytics.
- Verify unsupported Community writes do not return fake success.

Regression verification must include:

- `BackendV21OpsRuntimeBridgeTest`
- `SuiteCollaborationCenterIntegrationTest`
- Notification and Command Center regression coverage from the existing Suite tests where practical
- `BackendV21AccessEntitlementGatesTest`
- `BackendV21ApiContractCatalogTest`
- `pnpm --dir frontend-v2 test`
- `pnpm --dir frontend-v2 typecheck`
- `pnpm --dir frontend-v2 build`

Backend test commands must use the repository-required `timeout 60s` wrapper.

## Acceptance Criteria

- Community v2 Collaboration, Notifications, and Command Center read routes use real Suite runtime data.
- Supported notification read-state mutation updates real Suite notification state.
- Premium operations endpoints remain blocked by the existing v2 access gate for Community users.
- Unsupported Community writes fail explicitly and do not fake success.
- New bridge code stays focused and below repository metrics limits.
- Existing Suite collaboration, notification, command center, access gate, and API catalog regressions continue to pass.
- Frontend v2 tests, typecheck, and build continue to pass.
- `docs/superpowers/progress/v21-implementation-progress.md` records the completed slice and verification evidence after implementation.

## Spec Self-Check

- Marker scan: no unresolved marker or incomplete section remains.
- Consistency check: route mappings match the current v2.1 contract catalog and existing Suite service capabilities.
- Scope check: this is one implementable runtime bridge slice; Admin, Billing, Settings, Public Share, Workspace summary, AI, MCP, and new persistence are explicitly excluded.
- Ambiguity check: unsupported Community writes are explicit failures, while supported reads and notification read-state mutations use real runtime services.
