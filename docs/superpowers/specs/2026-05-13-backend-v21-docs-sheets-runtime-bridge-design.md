# Backend v2.1 Docs and Sheets Runtime Bridge Design

Date: 2026-05-13

## Context

The previous backend v2.1 slices completed API contract publication, event outbox foundation, background jobs, access entitlement gates, Calendar runtime bridge, and Drive runtime bridge. The next unimplemented core workspace contracts with existing backend runtime services are Docs and Sheets.

Frontend v2.1 already calls `/api/v2/docs*` and `/api/v2/sheets*` from shared API clients and editor surfaces. Backend v1 already has real runtime behavior through `DocsController`, `DocsService`, `DocsCollaborationService`, `SheetsController`, `SheetsService`, and `SheetsVersionService`.

## Decision

Implement one combined slice: `backend-v21-docs-sheets-runtime-bridge`.

This slice bridges the Community Docs and Sheets v2.1 contracts to the existing runtime services. It does not add fake success paths for Premium features or unsupported import semantics. Premium routes continue to be blocked by `V21ApiAccessGateInterceptor` through the existing contract catalog and entitlement provider.

## Scope

In scope:

- Add `V21DocsController` for v2 Docs runtime routes.
- Add `V21SheetsController` for v2 Sheets runtime routes.
- Add small v2 request records only where the existing v1 DTOs cannot be safely reused from the v2 route shape.
- Add focused integration coverage for Docs and Sheets v2 runtime behavior.
- Update v2.1 progress after implementation.

Out of scope:

- New Docs version persistence.
- Fake Docs version lists derived from `currentVersion`.
- JSON-based Sheets import parsing from `format` and `content`.
- Premium Sheets cleaning rules or AI insights execution.
- Broad frontend redesign or visual parity changes.

## Architecture

The v2 controllers are thin adapters:

- They own v2 route paths and HTTP method semantics.
- They resolve the authenticated principal through the existing security utilities.
- They adapt request payloads to existing service method parameters.
- They return existing VO models through `Result.success`.

Business rules remain in existing services. The bridge must not duplicate access checks, collaboration rules, audit behavior, version conflict checks, or workbook state logic that already exists in `DocsService`, `DocsCollaborationService`, `SheetsService`, and `SheetsVersionService`.

## Docs Route Mapping

| v2 route | Runtime mapping | Entitlement |
| --- | --- | --- |
| `GET /api/v2/docs` | `DocsService.list(userId, keyword, limit)` | Community |
| `POST /api/v2/docs` | `DocsService.create(...)` | Community |
| `GET /api/v2/docs/{id}` | `DocsService.get(userId, noteId)` | Community |
| `PATCH /api/v2/docs/{id}` | `DocsService.update(...)` | Community |
| `GET /api/v2/docs/{id}/comments` | `DocsCollaborationService.listComments(...)` | Community |
| `POST /api/v2/docs/{id}/comments` | `DocsCollaborationService.createComment(...)` | Community |
| `POST /api/v2/docs/{id}/share` | `DocsCollaborationService.createShare(...)` | Community |
| `GET /api/v2/docs/{id}/versions` | Blocked before controller by Premium gate | Premium |

`PATCH /api/v2/docs/{id}` requires a title, content, and current version. Missing or invalid fields surface as explicit validation errors.

## Sheets Route Mapping

| v2 route | Runtime mapping | Entitlement |
| --- | --- | --- |
| `GET /api/v2/sheets` | `SheetsService.list(userId, limit)` | Community |
| `POST /api/v2/sheets` | `SheetsService.create(...)` | Community |
| `GET /api/v2/sheets/{id}` | `SheetsService.get(userId, workbookId)` | Community |
| `PATCH /api/v2/sheets/{id}` | `SheetsService.updateCells(...)` | Community |
| `POST /api/v2/sheets/{id}/imports` | Explicit `INVALID_ARGUMENT` until real JSON import is designed | Community |
| `POST /api/v2/sheets/{id}/cleaning-rules` | Blocked before controller by Premium gate | Premium |
| `GET /api/v2/sheets/{id}/insights` | Blocked before controller by Premium gate | Premium |

`PATCH /api/v2/sheets/{id}` requires current version, sheet id, and cell edits. The existing workbook service remains responsible for version conflict detection, formula recomputation, state persistence, and snapshot creation.

## Error Handling

No silent fallback is introduced.

- Unknown or Premium routes keep using the v2 access gate.
- Missing required fields produce explicit validation failures.
- Sheets JSON import returns an explicit invalid-argument response because the existing backend import is multipart-based and cannot be truthfully bridged from the current v2 JSON client shape.
- Service-level ownership, permission, version conflict, and not-found errors remain the source of truth.

## Tests

Add `BackendV21DocsSheetsRuntimeBridgeTest` before implementation as a red test. It should cover:

- Docs create, list, read, update, comments, and share through `/api/v2/docs*`.
- Docs Premium versions route returning `V2_ENTITLEMENT_REQUIRED` in Community.
- Sheets create, list, read, and cell update through `/api/v2/sheets*`.
- Sheets JSON import returning explicit `INVALID_ARGUMENT`.
- Sheets Premium cleaning rules and insights returning `V2_ENTITLEMENT_REQUIRED` in Community.

Regression verification must include:

- `BackendV21DocsSheetsRuntimeBridgeTest`
- `DocsCollaborationIntegrationTest`
- `SheetsWorkbookIntegrationTest`
- `SheetsSharingVersionIntegrationTest`
- `BackendV21AccessEntitlementGatesTest`
- `BackendV21ApiContractCatalogTest`
- `pnpm --dir frontend-v2 test`
- `pnpm --dir frontend-v2 typecheck`
- `pnpm --dir frontend-v2 build`

Backend test commands must use the repository-required `timeout 60s` wrapper.

## Acceptance Criteria

- Community Docs and Sheets v2 routes use real persisted runtime data.
- v2 Docs and Sheets editor save flows can hit backend v2 routes without relying on v1 URLs.
- Premium Docs and Sheets routes are explicit locked boundaries, not fake responses.
- Existing v1 Docs and Sheets integration tests continue to pass.
- `docs/superpowers/progress/v21-implementation-progress.md` records the completed slice and verification evidence after implementation.

## Spec Self-Check

- Marker scan: no unresolved marker or incomplete section remains.
- Consistency check: route mappings match the current v2.1 contract catalog and the existing v1 service capabilities.
- Scope check: this is one implementable runtime bridge slice; unsupported import and Premium features are explicitly excluded.
- Ambiguity check: Sheets JSON import is intentionally rejected until a real import parser slice is designed.
