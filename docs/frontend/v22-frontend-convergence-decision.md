# v2.2 Frontend Convergence Decision

Date: 2026-05-17

## Decision

Decision: keep `frontend-v2` as a v2.2 legacy reference only, then remove or archive it in v2.3.

The selected contracts have moved to root tests and `frontend-admin`. `frontend-admin` is the only product frontend. No new feature, bugfix, dependency bump, style change, or migration shim may enter `frontend-v2`; only deletion or rename-out archival is allowed.

Selected legacy contracts have moved into `tests/v22-legacy-frontend-contract-migration.test.mjs` and the `frontend-admin` public share surface.

## Rationale

- `frontend-admin` already carries the product shell, v2.1.2 contracts, coverage, e2e, bundle, i18n, style discipline and deployment path.
- `frontend-v2` still contains historical route, public share, workspace and visual QA files that are useful as archival evidence.
- Selected auth, workspace, public share, settings and command-center contracts now have root or `frontend-admin` coverage.
- Deleting `frontend-v2` during v2.2 would create review noise; v2.3 should delete or archive it deliberately.

## Rules

- Runtime, Compose, Helm, image publishing, install docs and release gate remain `frontend-admin` only.
- `frontend-v2` may only lose files or move historical material out to `frontend-admin/tests` or root `tests`.
- `scripts/validate-legacy-frontend-v2-freeze.sh` blocks new or modified `frontend-v2` files.
- The retired legacy migration signal must not be reintroduced.
- The retired CI legacy migration job must not be reintroduced.
- Any change touching `frontend-v2` must explain which contract is being removed or migrated.
- Historical plans, progress logs, v2.0/v2.1 specs, and old release notes may mention `frontend-v2`; those references are archival evidence only and must not be used as current product gates.

## Exit Criteria

- All selected legacy contracts have equivalents under `frontend-admin/tests` or root `tests`.
- The retired legacy migration signal is removed.
- CI no longer contains the retired legacy migration job.
- `frontend-v2` is deleted or moved to `archive/frontend-v2`.
