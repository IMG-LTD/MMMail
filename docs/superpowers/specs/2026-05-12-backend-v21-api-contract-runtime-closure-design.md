# Backend v2.1 API Contract Runtime Closure Design

## 1. Purpose

This specification defines the next v2.1 slice after the frontend visual, interaction, and design-parity closure work.

The frontend v2.1 surface now has route metadata, visual QA evidence, design parity tracking, and API-boundary contract tests. The next risk is backend alignment: the runtime `/api/v2/platform/contracts` catalog, immutable Java contract records, and frozen OpenAPI catalog must stay consistent with the v2.1 frontend namespaces and module ownership rules.

This slice closes the backend v2.1 API contract runtime, without adding fake business API responses or mock success paths.

## 2. Current Baseline

Current repository state:

- Branch: `main`
- Latest frontend v2.1 implementation commit: `f98e8c14 test(frontend-v2): close v2.1 design parity audit`
- Latest progress commit: `4878eacf docs(frontend-v2): update v2.1 design parity progress`
- Frontend visual QA: `69` screenshots
- Frontend v2.1 tests: `83/83` passing in the latest recorded run

Existing backend contract files:

- `backend/mmmail-platform/src/main/java/com/mmmail/platform/contract/V21ApiContractCatalog.java`
- `backend/mmmail-platform/src/main/java/com/mmmail/platform/contract/V21ApiContract.java`
- `backend/mmmail-platform/src/main/java/com/mmmail/platform/contract/V21ApiContractMetadata.java`
- `backend/mmmail-platform/src/main/java/com/mmmail/platform/contract/V21ApiAccess.java`
- `backend/mmmail-platform/src/main/java/com/mmmail/platform/contract/V21ApiOwner.java`
- `backend/mmmail-platform/src/main/java/com/mmmail/platform/contract/V21ApiSchema.java`
- `backend/mmmail-server/src/main/java/com/mmmail/server/controller/V21ApiContractCatalogController.java`
- `backend/mmmail-server/src/test/java/com/mmmail/server/BackendV21ApiContractCatalogTest.java`
- `contracts/openapi/v21-api-catalog.yaml`

The existing catalog already exposes `/api/v2/platform/contracts`, but the next slice should make the closure explicit and auditable across runtime payload, OpenAPI catalog, and v2.1 progress documentation.

## 3. Design Sources

Primary sources:

- `docs/superpowers/specs/2026-04-28-frontend-v21-ui-upgrade-design.md`
- `docs/superpowers/specs/2026-04-28-backend-v21-architecture-design.md`
- `docs/superpowers/plans/2026-05-11-backend-v21-api-contracts-and-data-models.md`
- `docs/superpowers/progress/v21-implementation-progress.md`
- `docs/superpowers/progress/v21-visual-parity-risk-register.md`
- `contracts/openapi/v21-api-catalog.yaml`
- Existing frontend v2.1 API boundary tests under `frontend-v2/tests/v21-*.test.mjs`

The source of truth for API families remains the v2.1 module namespace list:

- `/api/v2/workspace/*`
- `/api/v2/mail/*`
- `/api/v2/calendar/*`
- `/api/v2/drive/*`
- `/api/v2/docs/*`
- `/api/v2/sheets/*`
- `/api/v2/pass/*`
- `/api/v2/collaboration/*`
- `/api/v2/command-center/*`
- `/api/v2/notifications/*`
- `/api/v2/admin/*`
- `/api/v2/settings/*`
- `/api/v2/labs/*`
- `/api/v2/billing/*`
- `/api/v2/entitlements/*`
- public/auth/share/system boundaries under `/api/v2/auth/*`, `/api/v2/share/*`, and `/api/v2/system/*`

## 4. Scope

In scope:

- Add or tighten backend tests that compare runtime catalog coverage with expected v2.1 API namespaces.
- Verify every runtime contract has method, path, owner module, response model, request model, permissions, entitlement, and design source.
- Verify `/api/v2/platform/contracts` requires authentication and returns the same contract shape as the Java catalog payload.
- Verify `contracts/openapi/v21-api-catalog.yaml` contains the same critical route identities and metadata extension fields.
- Update v2.1 progress documentation with backend contract runtime closure evidence.

Out of scope:

- Implementing real business endpoints for every `/api/v2/*` route.
- Returning fake or simulated business payloads.
- Changing existing `/api/v1/*` behavior.
- Adding database schema migrations.
- Adding external queue, worker, or service-extraction infrastructure.
- Broad backend module refactors unrelated to API contract runtime closure.

## 5. Runtime Contract Model

The runtime catalog should continue to use immutable Java records:

- `V21ApiContractCatalog`: version plus immutable contract list.
- `V21ApiContract`: method, path, owner, schema, and access metadata.
- `V21ApiOwner`: module and design source.
- `V21ApiSchema`: response and request model names.
- `V21ApiAccess`: permission list and entitlement label.

The endpoint remains:

```text
GET /api/v2/platform/contracts
```

It must stay authenticated. The endpoint is a contract catalog, not a public discovery endpoint for anonymous users.

## 6. Coverage Model

The backend catalog should expose coverage for each frontend v2.1 product family:

| Product family | Backend owner signal |
| --- | --- |
| Workspace | `workspace` |
| Mail | `mail` |
| Calendar | `calendar` |
| Drive | `drive` |
| Docs | `docs` |
| Sheets | `sheets` |
| Labs | `labs` |
| Pass | `pass` |
| Collaboration | `collaboration` |
| Command Center | `command-center` |
| Notifications | `notifications` |
| Admin/Governance | `admin-governance` |
| Settings | `settings` |
| Auth | `identity` |
| Public Share | `public-share` |
| System | `system` |

If billing and entitlement-specific runtime contracts are not present in this slice, the implementation must either add them to the catalog or document why their v2.1 coverage is represented through existing governance/billing readiness contracts. The implementation plan should start with a failing test that makes this decision explicit.

## 7. Test Design

The implementation should use targeted backend tests before any code changes.

Recommended test additions:

- Assert the catalog contains every required owner module.
- Assert the catalog contains route prefixes for all v2.1 API families.
- Assert all entries have non-empty permission and entitlement metadata.
- Assert all design sources point to either `docs/MMMail/UI/*` or an explicitly documented system/public boundary.
- Assert the authenticated runtime payload exposes `version`, `moduleCount`, and `contracts`.
- Assert anonymous access to `/api/v2/platform/contracts` remains `401`.
- Assert `contracts/openapi/v21-api-catalog.yaml` includes `x-permission`, `x-entitlement`, and `x-design-source`.

Required verification commands:

```bash
timeout 60s pnpm --dir frontend-v2 test
timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml test -Dtest=BackendV21ApiContractCatalogTest
timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml compile
```

If Maven targeted tests need more than 60 seconds due Spring context startup, report the timeout evidence and use the repository’s existing backend verification command only after documenting the reason in the implementation progress.

## 8. Progress Tracking

Update `docs/superpowers/progress/v21-implementation-progress.md` after implementation with:

- Slice: `backend-v21-api-contract-runtime-closure`
- Implementation commit hash.
- Files changed.
- Backend targeted test outcome.
- Maven compile outcome.
- Frontend v2.1 contract test outcome.
- Any remaining backend v2.1 contract risks.

Do not add `.tmp/`, generated target artifacts, or unrelated untracked paths to commits.

## 9. Acceptance Criteria

This slice is complete when:

- Backend contract tests prove the runtime catalog covers the v2.1 API families.
- `/api/v2/platform/contracts` remains authenticated.
- Runtime catalog metadata includes permissions, entitlements, request/response model names, and design-source mapping.
- OpenAPI catalog contains matching v2.1 catalog metadata fields.
- v2.1 progress documentation records the backend contract closure.
- Targeted backend tests, Maven compile, and frontend v2.1 tests pass or any timeout is explicitly recorded with the exact command and output.
- No fake business API response path is introduced.

## 10. Self-Check

- No business endpoint implementation is required by this design.
- No silent fallback, mock success, or anonymous catalog exposure is allowed.
- The work is one implementation slice and can be covered by a single writing-plans document.
- The design follows existing backend `V21ApiContract*` records instead of inventing a parallel contract model.
