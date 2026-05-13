# Backend v2.1 Drive Runtime Bridge Design

## Purpose

MMMail v2.1 frontend Drive already consumes `/api/v2/drive/*` routes, while the backend has mature real Drive behavior under `/api/v1/drive/*`. This slice closes that runtime gap by adding a v2 Drive controller that reuses the existing `DriveService` domain behavior instead of adding fake success paths or silent v1 fallbacks.

The slice is intentionally narrow: Drive runtime only. It must not broaden into Mail, Docs, Sheets, Pass, Collaboration, Command Center, Notifications, Admin, or Settings runtime bridges.

## Current Context

- Frontend Drive calls `/api/v2/drive/files`, `/api/v2/drive/folders`, `/api/v2/drive/uploads`, `/api/v2/drive/files/:id/share`, `/api/v2/drive/files/:id/versions`, and `/api/v2/drive/storage/summary`.
- Backend v1 Drive already supports item listing, file metadata creation, multipart upload, rename, move, delete, usage, sharing, versions, public shares, and collaboration shares through `DriveController` and `DriveService`.
- Backend v2 access entitlement gates now enforce `/api/v2/*` contract metadata before controller fallback.
- The v2 contract catalog marks core Drive read/write/share endpoints as Community and file versions as Premium.
- `frontend-v2/src/views/app/drive/DriveSharePanel.vue` still contains local-only share action wording because real v2 Drive share mutations are not bridged yet.

## Goals

1. Provide real `/api/v2/drive/*` runtime endpoints for Community Drive list, usage, metadata upload, update, delete, and share workflows.
2. Reuse existing v1 Drive domain services, validation, persistence, quota checks, audit events, and authorization behavior.
3. Keep Premium file version endpoint entitlement-gated in Community instead of returning fake version data.
4. Remove or replace Drive-scope dead code, unused declarations, and local-only placeholder behavior touched by this bridge.
5. Preserve existing v1 Drive release-blocking behavior and v2 access gate behavior.

## Non-Goals

- No new storage engine, multipart upload queue, preview pipeline, or background file processing.
- No fake upload status object or simulated async queue.
- No bypass of `V21ApiAccessGateInterceptor`.
- No broad v2 bridge for non-Drive modules.
- No unrelated frontend redesign or visual layout rewrite.
- No sweeping cleanup outside files touched by the Drive runtime bridge.

## Backend API Design

Add a v2 Drive controller under `/api/v2/drive`.

### Community Runtime Endpoints

- `GET /api/v2/drive/files`
  - Returns `List<DriveItemVo>`.
  - Reuses `DriveService.listItems` with `itemType=FILE`.
  - Supports existing `parentId`, `keyword`, and `limit` query semantics.
- `GET /api/v2/drive/folders`
  - Returns `List<DriveItemVo>`.
  - Reuses `DriveService.listItems` with `itemType=FOLDER`.
  - Supports existing `parentId`, `keyword`, and `limit` query semantics.
- `GET /api/v2/drive/storage/summary`
  - Returns `DriveUsageVo`.
  - Reuses `DriveService.usage`.
- `POST /api/v2/drive/uploads`
  - Accepts the frontend v2 JSON payload `{ fileName, parentId, sizeBytes }`.
  - Maps it to existing metadata-backed file creation through `CreateDriveFileRequest`.
  - Does not claim that binary content was uploaded.
- `GET /api/v2/drive/uploads/{id}`
  - Returns the existing `DriveItemVo` for the created item.
  - Uses the persisted Drive item as the upload status source.
- `PATCH /api/v2/drive/files/{id}`
  - Accepts a small update payload for `name` and `parentId`.
  - Reuses `DriveService.renameItem` when `name` is present.
  - Reuses `DriveService.moveItem` when `parentId` is present.
  - Fails clearly when the request contains no supported field.
- `DELETE /api/v2/drive/files/{id}`
  - Reuses `DriveService.deleteItem`.
- `GET /api/v2/drive/files/{id}/share`
  - Returns `List<DriveShareLinkVo>`.
  - Reuses `DriveService.listShares`.
  - This fills the frontend-read gap while keeping the existing POST contract intact.
- `POST /api/v2/drive/files/{id}/share`
  - Accepts `CreateDriveShareRequest`.
  - Reuses `DriveService.createShare`.

### Premium-Gated Endpoint

- `GET /api/v2/drive/files/{id}/versions`

This remains a contract-listed Premium route. In Community it must be stopped by the v2 entitlement gate with `V2_ENTITLEMENT_REQUIRED` before business execution. This slice must not create fake version output for Community users.

## Frontend Design

Keep the existing Drive route and layout. The backend bridge should make current reads load real runtime state without adding a new visual redesign.

Drive-scope frontend cleanup is allowed where it directly follows from the bridge:

- Remove local-only copy that says share changes are not persisted if the action is wired to a real v2 endpoint in this slice.
- Remove unused Drive API helpers or imports discovered while wiring real runtime calls.
- Keep errors explicit through existing `resolveRuntimeError` behavior.
- Do not add silent fallback to v1 routes.

If share revoke remains outside the v2 contract catalog, the UI must keep that action clearly local or disabled instead of pretending the backend persisted it.

## Data Flow

```text
DriveSectionView.vue / drive.ts
  -> /api/v2/drive/*
  -> V21ApiAccessGateInterceptor
  -> V21DriveController
  -> DriveService
  -> existing MyBatis mappers, drive tables, storage quota, audit services
```

The v2 controller is an API compatibility bridge. Existing Drive service methods remain the source of business truth.

## Error and Access Behavior

- Missing or invalid authentication returns `UNAUTHORIZED`.
- Community access is allowed for Drive files, folders, upload metadata, update, delete, share, and storage summary routes.
- Premium file versions return `V2_ENTITLEMENT_REQUIRED` in Community.
- Missing or unauthorized files return existing Drive service errors.
- Invalid IDs, empty updates, unsupported update fields, duplicate names, invalid parents, and quota failures surface as explicit backend errors.
- No controller should catch and convert domain failures into fake success payloads.

## Cleanup Rules

Cleanup is required but scoped:

- Delete dead code introduced by previous Drive placeholders when it is replaced by real runtime behavior.
- Remove unused imports, unused helpers, and stale tests touched by the bridge.
- Remove obsolete local-only UI wording only after the corresponding backend call is real.
- Do not delete unrelated legacy v1 endpoints, tests, migrations, or public-share compatibility paths.
- Do not hide failing behavior behind defensive defaults or fallback payloads.

## Tests

Add backend coverage:

- `BackendV21DriveRuntimeBridgeTest`
  - creates a Drive file metadata item through `/api/v2/drive/uploads`,
  - lists it through `/api/v2/drive/files`,
  - reads folders through `/api/v2/drive/folders`,
  - reads usage through `/api/v2/drive/storage/summary`,
  - lists shares through `GET /api/v2/drive/files/{id}/share`,
  - creates a real share through `POST /api/v2/drive/files/{id}/share`,
  - updates a file name through `PATCH /api/v2/drive/files/{id}`,
  - deletes the file through `DELETE /api/v2/drive/files/{id}`,
  - verifies Premium versions route is denied by entitlement in Community.

Regression coverage must include:

- `DriveReleaseBlockingIntegrationTest`
- `BackendV21AccessEntitlementGatesTest`
- `BackendV21DriveRuntimeBridgeTest`
- `frontend-v2` test suite
- `frontend-v2` typecheck
- `frontend-v2` build

## Acceptance Criteria

- `/api/v2/drive/files` and `/api/v2/drive/folders` return real persisted Drive state.
- `/api/v2/drive/uploads` creates a real metadata-backed Drive item and does not fake binary upload completion.
- `/api/v2/drive/storage/summary` returns real quota and usage state.
- `/api/v2/drive/files/{id}/share` supports real share list and create behavior.
- `/api/v2/drive/files/{id}/versions` does not fake success in Community.
- Drive-scope dead code and obsolete placeholder behavior touched by the bridge are removed.
- Existing v1 Drive release-blocking behavior does not regress.
- All targeted backend and frontend validations pass before implementation is considered complete.

## Scope Boundary

This specification is sized for one implementation plan. If implementation reveals that true binary upload queue semantics are required for `/api/v2/drive/uploads/{id}`, that queue must become a follow-up slice rather than expanding this bridge.
