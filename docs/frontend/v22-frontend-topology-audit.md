# v2.2 Frontend Topology Audit

Date: 2026-05-16

## Decision

`frontend-admin` is the only product frontend for v2.2. `frontend-v2` remains in the repository only as a frozen legacy reference until v2.3 deletion or archive.

The convergence decision is recorded in `docs/frontend/v22-frontend-convergence-decision.md`: v2.2 keeps `frontend-v2` as a legacy reference only, while v2.3 removes or archives it.

Selected legacy contracts have moved into `tests/v22-legacy-frontend-contract-migration.test.mjs` and `frontend-admin`.

## Current Inventory

| Area | `frontend-admin` | `frontend-v2` | v2.2 treatment |
|---|---:|---:|---|
| View files | 40 | 71 | New product work only enters `frontend-admin`. |
| Test / e2e files | 69 | 56 | Product gates run `frontend-admin`; migrated legacy contracts run from root tests or `frontend-admin`. |
| Route files | `src/router/**` plus generated Elegant Router types | `src/app/router/**` and route surface fixtures | Route/access contracts must migrate before deleting legacy code. |
| Runtime deployment | Dockerfile and compose frontend service | No compose image | Do not publish `frontend-v2` as a runtime image. |

## CI And Gate Findings

Before this audit, CI and release gates still treated both frontends as current release surfaces. That was misleading for open-source users because deployment docs already point to one web runtime.

Current split:

- Product CI job: installs and validates `frontend-admin`.
- Release gate: typecheck, lint, format, coverage, e2e, bundle, i18n, and style checks target `frontend-admin`.
- Local validation: `scripts/validate-local.sh` validates the product frontend, required governance/runtime files and legacy freeze gate.
- Legacy contract migration gate: `node --test tests/v22-legacy-frontend-contract-migration.test.mjs` proves selected contracts moved to root tests or `frontend-admin`.
- Legacy freeze gate: `scripts/validate-legacy-frontend-v2-freeze.sh` blocks new or modified `frontend-v2` files.

The retired legacy migration signal must not be reintroduced. The remaining guard is a freeze rule, not a second product frontend.

Historical `docs/superpowers/`, v2.0/v2.1 specs, and old v2.0 release notes may still mention `frontend-v2` commands because they record earlier implementation and release states. They are not current release gates, runtime topology, or product frontend instructions.

## Legacy Migration List

Moved useful `frontend-v2` contracts before deleting or archiving the directory:

- Migrated selected contracts: auth scope, public share runtime/view, mail, drive, pass, workspace aggregation, settings panel, and command-center query.
- Current migration evidence: `tests/v22-legacy-frontend-contract-migration.test.mjs`, `frontend-admin/src/service/api/public-share.ts`, `frontend-admin/src/typings/api/public-share.d.ts`, and `frontend-admin/src/views/share/index.vue`.
- Stale or brittle legacy-only files are not v2.2 product gates. If any are found useful before v2.3 archive/delete, migrate them explicitly instead of deleting assertions for convenience.

The full `frontend-v2` test suite is stale and is not a product release signal.

## Required Rules

- Do not add new product functionality to `frontend-v2`.
- Do not add `frontend-v2` back to compose, Docker image publishing, release-gate typecheck/lint/format, or product install docs.
- Do not reintroduce the retired legacy migration signal or retired CI legacy migration job.
- `scripts/validate-legacy-frontend-v2-freeze.sh` blocks new or modified `frontend-v2` files; only deletion or rename out of `frontend-v2` is allowed during migration.
- Keep upstream attribution for the Soybean Admin template in `NOTICE`; do not expose Soybean metadata as MMMail product metadata.

## Acceptance

- README and install docs describe `frontend-admin` as the product frontend.
- `rg -n "frontend-v2" README.md docs/ops docs/release scripts .github` only returns legacy, migration, historical release-note, or security-scan exclusions.
- `scripts/release-gate.sh` does not run `pnpm --dir frontend-v2` unconditionally.
- `.github/workflows/ci.yml` does not contain a retired legacy migration job.
- `scripts/validate-local.sh` does not call any retired legacy migration signal.
