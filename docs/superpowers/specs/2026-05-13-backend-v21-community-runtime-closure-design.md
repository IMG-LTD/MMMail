# Backend v2.1 Community Runtime Closure Design

## Context

The latest completed backend slice is `backend-v21-collaboration-write-runtime`. It added real v2 Collaboration project, task, comment persistence and wired those writes through `V21OpsController`.

The remaining highest-value v2.1 backend gap is the Community runtime surface already called by `frontend-v2` but not yet implemented under matching `/api/v2` controllers:

- `frontend-v2/src/service/api/workspace.ts`
- `frontend-v2/src/service/api/settings.ts`
- `frontend-v2/src/service/api/entitlements.ts`

Existing backend coverage has the contract catalog and gate metadata for these endpoints, but only partial capability endpoints exist today, such as `/api/v2/workspace/aggregation` and `/api/v2/billing/readiness`.

## Goal

Implement a focused Community runtime closure for Workspace, Settings, and Entitlements so the v2.1 frontend can call real backend endpoints without relying on placeholder responses.

The slice name is:

`backend-v21-community-runtime-closure`

## In Scope

Workspace Community endpoints:

- `GET /api/v2/workspace/summary`
- `GET /api/v2/workspace/activity`
- `GET /api/v2/workspace/tasks`
- `PATCH /api/v2/workspace/tasks/{id}`

Settings Community endpoints:

- `GET /api/v2/settings/profile`
- `PATCH /api/v2/settings/profile`
- `GET /api/v2/settings/security`
- `PATCH /api/v2/settings/security`
- `GET /api/v2/settings/devices`
- `DELETE /api/v2/settings/devices/{id}`
- `GET /api/v2/settings/notifications`
- `PATCH /api/v2/settings/notifications`

Entitlement Community endpoints:

- `GET /api/v2/entitlements`
- `GET /api/v2/entitlements/matrix`

## Out of Scope

This slice does not implement real premium, hosted, or enterprise-governance business execution:

- `GET/PATCH /api/v2/settings/integrations`
- `GET /api/v2/settings/audit`
- `/api/v2/billing/*`
- `/api/v2/admin/*`
- Command Center runs and workflows
- Notification rules, templates, send, and analytics

Those routes remain controlled by the existing entitlement gate and contract catalog.

## Architecture

Add three small v2 bridge services and controllers.

### Workspace

`V21WorkspaceController` handles `/api/v2/workspace`.

`V21WorkspaceRuntimeBridgeService` builds v2 Workspace read models from existing real data:

- Product cards summarize Community product availability and last activity.
- Activity rows come from real existing sources in this priority order: persisted v2 Collaboration activity, suite collaboration center activity, and command-center notification activity. If all sources are empty, the endpoint returns an empty list.
- Tasks are limited to real Community actionable records. The first patchable task family is persisted v2 Collaboration tasks from `V21CollaborationWriteService`.

Workspace task IDs must encode their source explicitly, for example `collaboration-task-<id>`. `PATCH /workspace/tasks/{id}` only accepts supported source IDs. Unsupported or malformed IDs return `BizException(ErrorCode.INVALID_ARGUMENT, ...)`.

PATCH semantics:

- `completed=true` maps supported tasks to `DONE`.
- `completed=false` maps supported tasks to `OPEN`.
- `title` updates only supported persisted task records.
- Empty patch bodies are rejected.

No in-memory task state and no mock task completion path are allowed.

### Settings

`V21SettingsController` handles `/api/v2/settings`.

`V21SettingsRuntimeBridgeService` adapts existing services instead of duplicating business rules:

- Profile uses `UserPreferenceService.getProfile` and `UserPreferenceService.updateProfile`.
- Devices use `AuthService.listSessions` and `AuthService.revokeSession`.
- Security settings expose the Community-supported account security state available in the current codebase: `mfaEnabled=false` and `recoveryEmail=null`. This is an explicit capability statement, not a persisted setting.
- Notification settings return explicit deterministic Community defaults: `emailDigest=true` and `productUpdates=true`. PATCH returns `ErrorCode.INVALID_ARGUMENT` in this slice because no existing persisted fields store these two values.

The notification settings PATCH endpoint must not accept a requested payload as saved until real persistence fields exist.

### Entitlements

`V21EntitlementsController` handles `/api/v2/entitlements`.

`V21EntitlementRuntimeBridgeService` derives response data from `V21ApiContractCatalog` and access metadata:

- Auth, share, public-share, and system public helper contracts are excluded from product entitlement rows. Other Community contracts return `state=available`.
- Premium, hosted, and enterprise-governance contracts return `state=locked`.
- `requiredPlan` is `premium`, `hosted`, or `enterprise-governance` for locked rows; Community rows use `null`.
- The matrix groups contract identities or stable feature keys by entitlement tier.

The service must not unlock premium, hosted, or enterprise-governance capabilities for Community users.

## Response Models

Reuse frontend-facing shapes already defined in `frontend-v2/src/service/api/*.ts`.

Workspace:

- `WorkspaceSummary`
- `WorkspaceSummaryProduct`
- `WorkspaceActivityItem`
- `WorkspaceTask`

Settings:

- `UserPreference`
- `SecuritySettings`
- `DeviceSession`
- `NotificationSettings`

Entitlements:

- `EntitlementState`
- `EntitlementMatrix`

Backend records should live under `com.mmmail.server.model.vo` and request DTOs under `com.mmmail.server.model.dto`.

## Error Handling

- Missing or malformed IDs return `ErrorCode.INVALID_ARGUMENT`.
- Attempts to patch unsupported workspace task sources return `ErrorCode.INVALID_ARGUMENT`.
- Attempts to revoke the current device through `/api/v2/settings/devices/{id}` must preserve existing `AuthService` behavior and reject it.
- Unsupported persistence for settings writes must fail explicitly. It must not return the requested payload as if it were saved.
- Premium, hosted, and governance access remains enforced by the existing v2 access gate.

## Testing

Add `BackendV21CommunityRuntimeClosureTest`.

Required coverage:

- Register a user and call Workspace summary, activity, and tasks successfully.
- Create a v2 Collaboration task, observe it in Workspace tasks, patch it through `/api/v2/workspace/tasks/{id}`, and verify Collaboration task state changed.
- Invalid Workspace task ID returns `INVALID_ARGUMENT`.
- Profile read and patch go through `/api/v2/settings/profile` and persist via `UserPreferenceService`.
- Devices list returns real active sessions and device delete delegates to `AuthService.revokeSession`.
- Current device delete is rejected.
- Security settings read returns explicit Community-supported state.
- Notification settings read returns explicit state; patch either persists through a real field or returns `INVALID_ARGUMENT` with no fake success.
- Entitlements list and matrix are derived from `V21ApiContractCatalog` and include Community available rows plus locked premium, hosted, and enterprise-governance rows.

Regression coverage:

- `BackendV21AccessEntitlementGatesTest`
- `BackendV21ApiContractCatalogTest`
- The latest backend v2 runtime bridge tests that share Workspace, Settings, or Entitlements contracts.

## Progress Tracking

After implementation, update `docs/superpowers/progress/v21-implementation-progress.md`:

- Add `Backend Community runtime closure (backend-v21-community-runtime-closure)` to completed slices.
- Set Latest Completed Backend Slice to this slice.
- Record real commit hashes and verification commands.

## Acceptance Criteria

- All in-scope `/api/v2` endpoints have concrete controller methods.
- Workspace, Settings, and Entitlements responses match frontend service contracts.
- PATCH and DELETE endpoints change real existing state or fail explicitly.
- No mock success paths, silent fallbacks, or broad defensive catch-and-return behavior are introduced.
- New or modified Java files stay within repository code-size limits.
- Targeted backend tests pass with a 60 second timeout.
