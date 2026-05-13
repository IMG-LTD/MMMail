# Backend v2.1 Pass Runtime Bridge Design

## Context

MMMail v2.1 frontend Pass surfaces already consume `/api/v2/pass/*` routes. The backend has real Pass behavior under `/api/v1/pass/*`, including personal vault items, aliases, mailboxes, secure links, business vaults, item sharing, and security monitor state.

This slice closes the v2 runtime gap by adding a thin v2 controller that reuses existing Pass domain services. It must not introduce fake success responses, mock data, silent v1 fallbacks, or new entitlement bypasses.

## Selected Approach

Use a complete v2 Pass bridge with existing entitlement boundaries.

Community endpoints will execute real personal Pass runtime behavior. Premium endpoints will be wired to real services where practical, but the v2 access gate remains authoritative and continues to block Community users before controller execution.

Rejected alternatives:

- Community-only bridge: faster, but leaves frontend Pass secure-link, alias, and monitor boundaries unverified.
- Broad Pass v1/v2 service refactor: cleaner long term, but too large for this runtime bridge slice.

## API Scope

Implement the v2 endpoints from the v2.1 master specification section 14.7:

- `GET /api/v2/pass/vaults`
- `GET /api/v2/pass/items`
- `POST /api/v2/pass/items`
- `PATCH /api/v2/pass/items/:id`
- `POST /api/v2/pass/share`
- `GET /api/v2/pass/secure-links`
- `POST /api/v2/pass/secure-links`
- `DELETE /api/v2/pass/secure-links/:id`
- `GET /api/v2/pass/aliases`
- `PATCH /api/v2/pass/aliases/:id`
- `GET /api/v2/pass/monitor`

The controller should not add v2 routes outside this list.

## Runtime Mapping

`V21PassController` will be the only new controller for this slice.

Community runtime mapping:

- `GET /vaults`: return a real personal vault summary derived from the current user and personal item count.
- `GET /items`: delegate to `PassService.list`.
- `POST /items`: delegate to `PassService.create`, then map `PassItemDetailVo` to the v2 summary shape expected by `frontend-v2/src/service/api/pass.ts`.
- `PATCH /items/:id`: parse the path id, delegate to `PassService.update`, then map detail to summary.

Premium runtime mapping:

- `GET /monitor`: delegate to `PassMonitorService.getPersonalMonitor` when the access gate allows the request.
- `GET /aliases`: delegate to `PassAliasService.listAliases` when allowed.
- `PATCH /aliases/:id`: delegate to `PassAliasService.updateAlias` when allowed.
- `GET /secure-links`: return secure-link dashboard data from existing Pass business services when a valid organization context is provided and the gate allows the request.
- `POST /secure-links` and `POST /share`: create real secure links through existing Pass business services when the request includes organization and item context and the gate allows it.
- `DELETE /secure-links/:id`: revoke through existing Pass business services when the request includes organization context and the gate allows it.

Community tests should assert Premium endpoints are blocked by the existing v2 access gate, not by custom controller guards.

## Adapter Types

Add only the minimal v2 adapter types needed to avoid controller parameter sprawl and front/back field mismatch:

- `V21PassVaultVo`: `id`, `name`, `scopeType`, `ownerEmail`, `itemCount`, `updatedAt`.
- `V21PassSecureLinkRequest`: request body for secure-link creation and share creation, including `orgId`, `itemId`, `maxViews`, and `expiresAt`.
- `V21PassSecureLinkQuery`: query binding for list/delete endpoints that require `orgId`.

Existing DTOs and VOs should be reused when the shape already matches:

- `CreatePassItemRequest`
- `UpdatePassItemRequest`
- `UpdatePassAliasRequest`
- `PassItemSummaryVo`
- `PassMonitorOverviewVo`
- `PassMailAliasVo`
- `PassSecureLinkVo` or `PassSecureLinkDashboardVo`

## Entitlement And Access Behavior

The v2 access gate remains the source of truth. This slice must not hardcode entitlement decisions in `V21PassController`.

Expected behavior:

- Community endpoints return real data for an authenticated user.
- Premium endpoints return `V2_ENTITLEMENT_REQUIRED` for a normal Community test user.
- Unknown `/api/v2/pass/*` routes continue to be handled by the existing v2 unknown-contract behavior.

## Error Handling

Failures must surface explicitly:

- Invalid numeric ids return `INVALID_ARGUMENT`.
- Missing or unauthorized Pass items surface the existing service error.
- Malformed JSON uses the global `HttpMessageNotReadableException` handling already added in the Mail runtime bridge slice.
- Secure-link operations without required `orgId` or `itemId` return `INVALID_ARGUMENT`.
- No endpoint returns empty objects, empty arrays, or success just to satisfy frontend shape.

## Test Strategy

Add `BackendV21PassRuntimeBridgeTest`.

Required coverage:

- Register a user and call `GET /api/v2/pass/vaults`; assert a personal vault exists and reflects real item count.
- Create a personal item through `POST /api/v2/pass/items`; assert returned v2 summary fields are populated.
- Read items through `GET /api/v2/pass/items`; assert the created item is persisted.
- Update the item through `PATCH /api/v2/pass/items/:id`; assert the updated title and item type are returned.
- Call `GET /api/v2/pass/monitor`, `GET /api/v2/pass/secure-links`, and `GET /api/v2/pass/aliases` as a Community user; assert `403` with `V2_ENTITLEMENT_REQUIRED`.
- Call a malformed id path such as `PATCH /api/v2/pass/items/not-a-number`; assert `INVALID_ARGUMENT`.

Regression verification:

- `BackendV21PassRuntimeBridgeTest`
- `BackendV21AccessEntitlementGatesTest`
- `BackendV21ApiContractCatalogTest`
- Existing Pass regression coverage around personal items, monitor, aliases, or business secure links as relevant.
- `pnpm --dir frontend-v2 test`
- `pnpm --dir frontend-v2 typecheck`
- `pnpm --dir frontend-v2 build`

## Progress Recording

After implementation, update `docs/superpowers/progress/v21-implementation-progress.md`:

- Add `Backend Pass runtime bridge (backend-v21-pass-runtime-bridge)` to completed slices.
- Set latest backend implementation commit to the implementation commit.
- Set active backend slice to completed and record verification evidence.

## Acceptance Criteria

- `/api/v2/pass` Community endpoints use real Pass runtime data.
- Premium Pass endpoints remain blocked by v2 access gate for Community users.
- No fake success, mock data, or silent fallback behavior is introduced.
- New files remain focused and under project size limits.
- All required backend and frontend verification commands pass before implementation is committed.
