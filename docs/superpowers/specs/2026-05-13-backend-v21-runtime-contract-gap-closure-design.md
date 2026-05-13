# Backend v2.1 Runtime Contract Gap Closure Design

## Context

MMMail v2.1 now has broad frontend contract coverage and most Community backend runtime bridges:

- Frontend v2.1 contract and visual QA suites are passing.
- Backend v2.1 API catalog, outbox, job foundation, access gates, and Community runtime bridges are implemented.
- Workspace, Settings, and Entitlements were closed in the latest backend Community runtime slice.

The remaining risk is cross-surface drift. Frontend v2 API clients, the backend `V21ApiContractCatalog`, and Spring MVC controllers can diverge independently. When they drift, the visible symptom is usually a late 404, missing entitlement gate, or an API client path that is not represented in the platform contract catalog.

This slice closes that gap by making drift testable before more feature work continues.

## Goal

Create a backend v2.1 runtime contract gap closure that verifies and repairs alignment across:

1. frontend `frontend-v2/src/service/api/*` `/api/v2/*` calls,
2. backend `V21ApiContractCatalog` entries,
3. backend controller mappings and entitlement gate behavior.

The outcome must expose unsupported functionality explicitly. It must not add mock success responses, silent fallbacks, or placeholder implementations.

## Selected Approach

Use a contract-audit-first slice.

The implementation should start with failing tests that compare known frontend v2 client paths against backend catalog and runtime behavior. Then it should fix only the gaps that the audit exposes.

This is preferred over continuing with one module at a time because v2.1 is now in a closure phase: isolated module work can leave hidden cross-module mismatches behind.

## Scope

### In Scope

- Add a focused backend test for frontend-client-to-catalog alignment.
- Add or tighten backend runtime smoke coverage for controller/gate availability.
- Register missing catalog entries when the frontend has a real v2 client path.
- Add explicit controller endpoints only when needed to avoid 404 for a cataloged v2 route.
- Route Community-supported endpoints to real existing services.
- Route Premium, Hosted, or Governance endpoints through explicit entitlement or unsupported errors.
- Update v2.1 progress documentation after implementation.

### Out of Scope

- Building full Hosted billing execution.
- Building full enterprise admin governance execution.
- Building full Premium AI, MCP, notification rules, or command execution.
- Adding fake payloads just to satisfy frontend screens.
- Changing frontend UI design unless a client path is proven wrong and must be corrected.

## Expected Gap Categories

The implementation plan should verify these categories rather than assume the exact final list:

- Frontend client path exists but backend catalog is missing it.
- Catalog path exists but Spring MVC has no mapping and no access gate outcome.
- Catalog marks a route as Community but the backend returns a premium or unsupported boundary.
- Catalog marks a route as Premium, Hosted, or Governance but the backend returns 404 instead of an explicit entitlement or unsupported error.
- Frontend calls a path that should be removed or normalized to an already cataloged route.

Known candidate areas from the current scan include auth session helpers, settings integrations/audit, AI platform capability reads, MCP registry capability reads, and public/system capability paths. These are candidates for verification, not pre-approved implementation scope.

## Runtime Rules

- Community endpoints must use real existing domain services or return a clear validation/error response when the requested operation is invalid.
- Premium, Hosted, and Governance endpoints must not succeed in Community by returning static placeholder data.
- Unsupported writes must fail with explicit `INVALID_ARGUMENT` or the existing entitlement error pattern, depending on whether the route is unsupported in Community or blocked by entitlement.
- Missing routes must be fixed at the controller/gate layer, not hidden by frontend fallback logic.

## Test Strategy

The implementation plan should add tests in this order:

1. A catalog alignment test that freezes frontend v2 client paths against `V21ApiContractCatalog`.
2. A runtime smoke test that proves cataloged Community routes are mapped.
3. A boundary smoke test that proves Premium, Hosted, and Governance routes fail explicitly rather than 404.
4. Regression coverage for any changed controller or service.

Existing tests that must remain green:

- `BackendV21ApiContractCatalogTest`
- `BackendV21AccessEntitlementGatesTest`
- `BackendV21CommunityRuntimeClosureTest`
- `BackendV21CollaborationWriteRuntimeTest`
- `BackendV21OpsRuntimeBridgeTest`
- `pnpm --dir frontend-v2 test`

## Error Handling

Errors should be intentionally visible:

- Unknown contracts return the existing v2 contract access error.
- Entitlement-blocked routes return the existing entitlement-required error.
- Unsupported Community runtime operations return explicit invalid-argument style errors.
- No new silent catch-and-default behavior should be introduced.

## Documentation

After implementation, update `docs/superpowers/progress/v21-implementation-progress.md`:

- Add `Backend Runtime contract gap closure (backend-v21-runtime-contract-gap-closure)` to completed slices.
- Set it as latest completed backend slice.
- Record implementation commits and verification commands.

## Acceptance Criteria

- A failing gap test is added before implementation changes.
- Frontend v2 API client paths and backend catalog entries are aligned or intentionally documented.
- Cataloged v2 routes have either real controller behavior or explicit entitlement/unsupported behavior.
- No newly introduced mock success paths or silent fallbacks exist.
- Targeted backend tests pass.
- Full frontend contract tests pass.
- Progress documentation is updated and committed.
