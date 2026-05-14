# Backend v2.1 Release Gate Hardening Design

## Context

MMMail v2.1 has completed the main frontend and backend runtime closure slices:

- Frontend v2.1 route, shell, shared component, visual QA, and contract suites are passing.
- Backend v2.1 API catalog, access gates, event outbox, job foundation, and runtime bridges are implemented.
- The latest backend runtime contract gap closure aligned frontend `/api/v2/*` clients, `V21ApiContractCatalog`, OpenAPI, auth runtime, AI/MCP capabilities, and public share runtime.

The remaining release risk is not missing runtime behavior. It is gate drift: the new `BackendV21*` tests exist, but the local and CI release gates still mostly run older v2 contract tests. A later change could break a v2.1 runtime bridge while `scripts/validate-local.sh` and GitHub Actions remain green.

## Goal

Create a release gate hardening slice that promotes the completed v2.1 backend regression suite into the same validation paths used for local release checks and CI.

The slice must make failures visible. It must not add skipped tests, best-effort fallbacks, mock success paths, or conditional bypasses to make validation pass.

## Selected Approach

Use gate-closure-first hardening.

Add one explicit v2.1 backend regression group to `scripts/validate-local.sh`, then mirror that group in `.github/workflows/ci.yml`. This is preferred over adding more runtime features because the current implementation surface is already broad; the highest value next step is ensuring it cannot regress silently.

## Alternatives Considered

### Option A: Gate closure first

Add all current `BackendV21*Test` classes to local and CI validation, then update progress documentation.

This is the recommended option because it directly closes the current release-process gap with a narrow blast radius.

### Option B: New business runtime slice

Continue with deeper notification, AI/MCP, or governance runtime work.

This adds visible capability, but it expands the surface before the new v2.1 tests are release-gated.

### Option C: Full `validate-local.sh` repair slice

Run and repair the entire local release validator.

This is valuable but too broad for this slice. Docker, dependency scan, and external environment gates can fail for reasons unrelated to v2.1 runtime coverage.

## Scope

### In Scope

- Add `BACKEND_V21_RUNTIME_TESTS` to `scripts/validate-local.sh`.
- Include every current backend v2.1 regression test class whose name starts with `BackendV21`.
- Add a dedicated `backend v2.1 runtime regression` validation stage using Maven and a 60 second timeout.
- Mirror the same `BackendV21*` class list in the GitHub Actions backend job.
- Update `docs/superpowers/progress/v21-implementation-progress.md` with this completed slice after implementation.
- Update the implementation plan with exact verification commands and commit steps.

### Out of Scope

- Adding new business endpoints.
- Changing frontend UI, route metadata, or visual QA behavior.
- Repairing unrelated Docker daemon, NVD, dependency scan, or container migration environment failures.
- Weakening existing validation gates.
- Introducing conditional skips based on local environment.

## Test Group

The v2.1 gate group should include the current backend v2.1 test classes:

- `BackendV21AccessEntitlementGatesTest`
- `BackendV21ApiContractCatalogTest`
- `BackendV21BackgroundJobFoundationTest`
- `BackendV21CalendarRuntimeBridgeTest`
- `BackendV21CollaborationWriteRuntimeTest`
- `BackendV21CommunityRuntimeClosureTest`
- `BackendV21DocsSheetsRuntimeBridgeTest`
- `BackendV21DriveRuntimeBridgeTest`
- `BackendV21EventOutboxFoundationTest`
- `BackendV21MailRuntimeBridgeTest`
- `BackendV21OpsRuntimeBridgeTest`
- `BackendV21PassRuntimeBridgeTest`
- `BackendV21RuntimeContractGapClosureTest`

If the implementation discovers another committed `BackendV21*Test` during execution, it should include it rather than hardcoding an obsolete list.

## Runtime And Failure Rules

- Maven failures must surface directly through a non-zero command exit.
- Test output can continue writing to `/tmp/mmmail-*.log`, matching the current validator style.
- Do not add `|| true`, optional skips, fallback test sets, or shortened test classes just to make the gate pass.
- Do not broaden this into full release validation repair unless the v2.1 test group itself exposes a root-cause failure.

## Documentation

After implementation, update `docs/superpowers/progress/v21-implementation-progress.md`:

- Add `Backend v2.1 release gate hardening (backend-v21-release-gate-hardening)` to completed slices.
- Set it as the latest completed backend slice.
- Record changed files, commits, and verification commands.
- Keep the existing untracked-path note unchanged unless the worktree changes.

## Verification

The implementation plan should require these checks:

1. Run the backend v2.1 runtime group directly:

   ```bash
   timeout 60s mvn -f backend/pom.xml -pl mmmail-server -am -Dtest=<BackendV21 group> -Dsurefire.failIfNoSpecifiedTests=false test
   ```

2. Run the frontend contract suite:

   ```bash
   timeout 60s pnpm --dir frontend-v2 test
   ```

3. If script changes are staged, run:

   ```bash
   bash -n scripts/validate-local.sh
   ```

4. Run `git diff --cached --check` before each commit.

## Acceptance Criteria

- `scripts/validate-local.sh` contains a dedicated v2.1 backend runtime regression stage.
- `.github/workflows/ci.yml` runs the same v2.1 backend regression group in CI.
- The direct backend v2.1 Maven command passes.
- `timeout 60s pnpm --dir frontend-v2 test` passes.
- Progress documentation records the new gate-hardening slice.
- No unrelated untracked paths are staged.

## Spec Self-Check

- Marker scan: no unresolved markers or open-ended requirements remain.
- Consistency check: local validation, CI validation, documentation, and acceptance criteria all reference the same gate-hardening scope.
- Scope check: the slice is limited to release gate coverage and progress documentation.
- Ambiguity check: full release validator repair is explicitly out of scope unless the new v2.1 test group itself fails.
