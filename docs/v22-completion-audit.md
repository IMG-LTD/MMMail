---
name: v2.2 completion audit
date: 2026-05-17
source_spec: docs/v22-open-source-commercial-spec.md
external_evidence: docs/v22-external-evidence-checklist.md
audit_status: not-complete-external-evidence-required
---

# v2.2 Completion Audit

## Objective Restatement

User objective:

> 先根据 `docs/v22-open-source-commercial-spec.md` 去执行完成全部任务，然后整理下再分析下仓库规范，对以上问题看下，还存在没有，一起修复了。

Concrete success criteria:

1. Execute the in-repository deliverables described by `docs/v22-open-source-commercial-spec.md`.
2. Keep every delivered capability backed by code, documentation, local validation, CI or release-gate evidence.
3. Analyze repository governance and fix in-repository governance drift.
4. Record remaining items as explicit evidence gaps, not vague unfinished work.
5. Do not mark the overall objective complete while external or live-environment evidence is still missing.

## Audit Method

This audit maps prompt requirements and spec gates to concrete repository artifacts. A passing test or verifier is accepted only when it covers the named requirement directly. External completion criteria are defined in `docs/v22-external-evidence-checklist.md`.

| Requirement | Evidence | Verification | Status |
|---|---|---|---|
| Current execution spec exists and remains the authority | `docs/v22-open-source-commercial-spec.md` | Root governance contract reads the spec and checks current status wording | Done |
| Repository governance files | `AGENTS.md`, `CODE_OF_CONDUCT.md`, `CONTRIBUTING.md`, `DCO.md`, `GOVERNANCE.md`, `MAINTAINERS.md`, `NOTICE`, `ROADMAP.md`, `SECURITY.md`, `SUPPORT.md` | `tests/v22-repository-governance-contract.test.mjs`, `tests/v22-repository-governance-validation-contract.test.mjs` | Done |
| DCO contribution path | `DCO.md`, `.github/workflows/dco.yml`, `CONTRIBUTING.md`, PR template | Root governance contract checks sign-off docs, common Git repair commands, no CLA wording, and GitHub API-based PR commit enforcement that avoids merge-commit false positives | Done |
| Versioned spec and new-code quality limits are explicit | `AGENTS.md`, `docs/v22-open-source-commercial-spec.md`, `CONTRIBUTING.md`, `.github/pull_request_template.md` | Root governance contract checks the spec line-count exception plus new-code 50-line function / 500-line regular source file limits, active source 500-line allowlist scanning, oversized legacy exceptions, split custom route registries, v2.2 commercial locale modules and `DrivePublicShareRateLimiter` extraction | Done |
| Single product frontend | `README.md`, ops docs, topology docs, `frontend-admin`, legacy freeze decision, `.github/dependabot.yml` | Root governance contract pair, `tests/v22-legacy-frontend-contract-migration.test.mjs`, `scripts/validate-legacy-frontend-v2-freeze.sh`; public entry docs now state legacy `frontend-v2` is frozen and may only lose files or move historical material out; legacy `frontend-v2` Dependabot version PRs are disabled with `open-pull-requests-limit: 0`; release-gate step 17 now runs the freeze gate with full CI checkout history | Done |
| Historical frontend-v2 docs boundary | `AGENTS.md`, `docs/frontend/v22-frontend-topology-audit.md`, `docs/frontend/v22-frontend-convergence-decision.md`, `docs/v22-open-source-commercial-spec.md` | Root governance contracts classify old `docs/superpowers/`, v2.0/v2.1 specs, and old release notes as archival evidence only | Done |
| Commercial public boundaries | `docs/commercial/pricing-boundaries.md`, `support-policy.md`, `trademark-policy.md`, `edition-entitlement-surface.md` | `tests/v22-commercial-boundaries-contract.test.mjs`, `BackendV22CommercialSurfaceCoverageContractTest` | Done |
| Edition, license, billing webhook, entitlement gates | Commercial backend classes, contracts, migrations `V36` to `V38` | `BackendV22EditionCoreContractTest`, `BackendV22LicenseVerifierContractTest`, `BackendV22BillingWebhookContractTest`, `BackendV22EntitlementEnforcementContractTest`, `BackendV22CommercialSurfaceCoverageContractTest` | Done in main repo |
| Billing contract schema | `contracts/billing/webhook-event.schema.json`, `contracts/license/license-claims.schema.json`, `docs/billing/webhook-signature.md`, `docs/billing/private-billing-evidence-template.md` | `BackendV22BillingWebhookContractTest`, root governance contract checks private billing gap remains external | Partial external billing repository required |
| Real payment and license signing | Public repo intentionally excludes payment credentials and signing private keys; `docs/billing/private-billing-evidence-template.md` defines required external evidence | Spec and commercial docs state this boundary; `gh repo view IMG-LTD/mmmail-billing-gateway` currently cannot resolve an accessible repository | External private billing repository required |
| OIDC SSO baseline | OIDC config/login/callback classes, `V39__oidc_sso_init.sql`, `docs/commercial/oidc-sso.md`, `docs/commercial/oidc-live-evidence-template.md` | `BackendV22OidcSsoContractTest`; includes the `OidcStateService` explicit runtime constructor regression; live evidence template defines the redacted BUS-01 / OBS-01 / GATE-01 evidence package but does not replace a real IdP run | Partial external evidence required |
| Audit JSONL export | Audit export controller/service and docs | `BackendV22AuditExportContractTest`, `tests/v22-audit-export-contract.test.mjs` | Done |
| DSR export/erasure/data inventory | DSR controller/service/docs/inventory | `BackendV22DsrContractTest`, `tests/v22-dsr-inventory-contract.test.mjs`, `scripts/validate-dsr-inventory.mjs` | Done |
| Helm deployment | `helm/mmmail`, `docs/ops/helm.md`, `scripts/validate-helm-chart.sh` | `tests/v22-deployment-helm-contract.test.mjs`, release-gate step 14 | Done |
| Image publishing workflow | `.github/workflows/images.yml`, release notes digest section, `docs/release/image-digest-evidence-template.md` | `tests/v22-image-publishing-contract.test.mjs`, release-gate step 15 | Partial external evidence required |
| Commercial self-host docs | `.env.example`, `config/backend.env.example`, `docs/ops/install.md`, `docs/ops/install.en.md`, `docs/ops/helm.md`, commercial boundary docs | Root and deployment contracts check env keys, Helm values, install docs and private billing boundary | Done in main repo |
| OpenTelemetry runtime tracing | `RuntimeTraceService`, request tracing, commercial span wrappers, `mmmail.oidc.callback` span docs | `BackendV22OpenTelemetryContractTest`, `tests/v22-observability-docs-contract.test.mjs` now blocks the old future/pending OIDC span wording | Partial external evidence required |
| SLI/SLO docs | `docs/observability/sli-slo.md` | `tests/v22-observability-docs-contract.test.mjs` | Done |
| Supply chain report | `scripts/generate-sbom-license-report.mjs` | `tests/v22-repository-governance-validation-contract.test.mjs`, release-gate step 13, CI artifact step | Done |
| Local validation artifact hygiene | Security scan scripts, CI validate job env, root shipping-cleanup contract | Local security reports default to `${TMPDIR:-/tmp}/mmmail-security`; CI explicitly sets `MMMAIL_SECURITY_REPORT_DIR=artifacts/security` before uploading reports, so local `validate-local` does not recreate repository-root `artifacts/` | Done |
| Generated type and diff hygiene | `frontend-admin/package.json`, `frontend-admin/scripts/normalize-generated-types.mjs`, `scripts/validate-local.sh` | `gen-route` and local validation normalize `elegant-router.d.ts` after route generation; `validate-local` ends with `git diff --check` so generated trailing whitespace cannot hide behind a green gate | Done |
| Security disclosure routing / GitHub private vulnerability reporting | `SECURITY.md`, `SUPPORT.md`, `.github/ISSUE_TEMPLATE/security-contact-request.md` | `gh api --method GET repos/IMG-LTD/MMMail/private-vulnerability-reporting` returned `{"enabled":true}`; root governance contract checks private reporting wording and fallback route | Done |
| Repository governance drift check | AGENTS, spec, root governance contracts, validate-local required files | `node --test tests/v22-repository-governance-contract.test.mjs tests/v22-repository-governance-validation-contract.test.mjs`; root contracts now fix the frozen `frontend-v2` wording in README, install docs, topology docs, CONTRIBUTING, PR template and CODEOWNERS; backend norms require explicit runtime constructor injection for multi-constructor Spring beans; active source line limits use an explicit legacy allowlist; local agent / validation artifacts stay ignored; root contract gate naming no longer claims v2.1.2-only coverage | Done |
| External evidence acceptance | `docs/v22-external-evidence-checklist.md`, `scripts/validate-v22-external-evidence.sh` | Root governance contract checks required external sources, non-evidence examples, expected verifier failure markers, and that default green gates do not run the failing verifier directly or indirectly through root tests; the verifier rechecks GitHub private vulnerability reporting via GitHub API, reports current read-only evidence gaps, and also requires completed evidence files with non-empty metadata fields, matching Public MMMail commit SHAs, an origin release tag pointing to that commit, remote branch containment, not templates, plus successful tag-push image workflow, GHCR package, and billing repo checks after incomplete markers are removed | Pending external evidence |

Verifier completed-state acceptance requires completed evidence files with non-empty metadata fields, not templates. It also requires those files to agree on the same Public MMMail commit, origin release tag commit, and remote branch containment.

## Verified Commands

The latest targeted verification for this audit:

| Command | Result |
|---|---|
| `node --test tests/v22-repository-governance-contract.test.mjs tests/v22-repository-governance-validation-contract.test.mjs` | 22 passed after pass-77 active source line-limit and local artifact ignore guardrails |
| `node --test tests/v22-dsr-inventory-contract.test.mjs tests/v22-repository-governance-contract.test.mjs tests/v22-repository-governance-validation-contract.test.mjs` | 24 passed |
| `node --test tests/*.test.mjs` | 83 passed after pass-77 governance-only additions |
| `pnpm --dir frontend-admin test:v212` | 124 passed, including v2.2 commercial license, billing, OIDC entry, entitlement localization, and commercial a11y gate contracts |
| `sg docker -c "cd /home/xiang/桌面/project/MMMail-test/MMMail && env -u http_proxy -u https_proxy -u HTTP_PROXY -u HTTPS_PROXY -u ALL_PROXY -u all_proxy pnpm --dir frontend-admin test:e2e"` | 11 passed; confirms backend Spring startup after the OIDC constructor fix |
| `pnpm --dir frontend-admin test:lighthouse` | Passed after a fresh build; `/login` Lighthouse performance=84 |
| `sg docker -c "cd /home/xiang/桌面/project/MMMail-test/MMMail && bash scripts/validate-batch3.sh"` | 9 passed, build success after V39 release metadata update |
| `sg docker -c "PATH=/tmp/mmmail-helm-bin:$PATH env -u http_proxy -u https_proxy -u HTTP_PROXY -u HTTPS_PROXY -u ALL_PROXY -u all_proxy bash scripts/validate-local.sh"` | `all checks passed` after the pass-77 repository governance changes; includes root repository contract gates, legacy frontend freeze, split route/locale/limiter governance, active source line-limit allowlist, generated type hygiene and `git diff --check`; confirms default local validation does not execute the manual-only external evidence verifier; local shell required Docker group refresh via `sg docker` and temporary Helm 3.14.4 on `PATH` |
| `pnpm --dir frontend-admin typecheck && pnpm --dir frontend-admin exec oxlint && pnpm --dir frontend-admin exec eslint . && pnpm --dir frontend-admin exec oxfmt --check && git diff --cached --check` | Passed after fixing pre-commit formatting; `vue-tsc`, `oxlint`, `eslint`, `oxfmt --check` and staged diff whitespace were clean |
| `git commit -s -m "feat(v22): implement open source commercial readiness"` | Created a local DCO-signed v2.2 commit; this audit refresh is amended into that same commit, and no push or tag was performed |
| `git diff --check` | Passed after generated type hygiene fix and pass-77 governance edits |
| `bash -n scripts/validate-local.sh scripts/validate-v22-external-evidence.sh` | Passed |
| `bash scripts/validate-v22-external-evidence.sh` | Failed as required while external evidence is incomplete; output lists 7 status markers and 8 read-only evidence gaps, including that the local v2.2 commit is not published to a remote commit/tag |
| `rg -n "[ \t]+$" frontend-admin/src/typings/elegant-router.d.ts AGENTS.md README.md CONTRIBUTING.md DCO.md .github/pull_request_template.md docs/frontend/v22-frontend-topology-audit.md docs/frontend/v22-frontend-convergence-decision.md docs/v22-open-source-commercial-spec.md docs/v22-completion-audit.md docs/v22-external-evidence-checklist.md scripts/validate-local.sh scripts/validate-v22-external-evidence.sh tests/v22-dsr-inventory-contract.test.mjs tests/v22-repository-governance-contract.test.mjs tests/v22-repository-governance-validation-contract.test.mjs` | No trailing whitespace matches |
| Final newline check for the same touched governance files | Passed |
| `wc -l scripts/validate-v22-external-evidence.sh tests/v22-repository-governance-contract.test.mjs tests/v22-repository-governance-validation-contract.test.mjs DCO.md` | External verifier is 426 lines; governance test files are 468 and 450 lines; `DCO.md` is 36 lines |
| `gh api --method GET repos/IMG-LTD/MMMail/private-vulnerability-reporting` | `{"enabled":true}` |
| `gh repo view IMG-LTD/mmmail-billing-gateway --json name,owner,visibility,isPrivate,defaultBranchRef,url` | Not found / not accessible; org repo list currently does not include `mmmail-billing-gateway` |
| `docker ps --format '{{.Names}}\t{{.Image}}\t{{.Ports}}'` | Docker daemon access denied in the normal shell |
| `sg docker -c "docker ps --format '{{.Names}}\t{{.Image}}\t{{.Ports}}'"` | Only `mmmail-nacos` and `mmmail-kafka` are running; no Keycloak/OIDC container evidence |
| `sg docker -c "docker compose ps --format json"` | Compose emitted missing-env warnings and did not produce live Keycloak/OIDC service evidence |
| `env | cut -d= -f1 | rg -i 'MMMAIL_OIDC|OIDC|KEYCLOAK|OTEL|MMMAIL_BILLING|MMMAIL_LICENSE'` | No matching environment variable names in the current shell |
| `ss -ltnp | rg '(:8080|:8081|:5173|:3000|:8443|:9000|:18080|:3306|:6379|:9725|:19527)'` | Only Java on `:8080` was visible; no Keycloak/OIDC listener evidence |
| `git rev-parse HEAD origin/main` | `HEAD` resolves to the local v2.2 commit while `origin/main` remains `e8903bf6c99c36fae7ee59f4ce039feb8d988bb3`; the v2.2 implementation is committed locally but has not been pushed to the remote default branch |
| `git status --short --branch` and `git rev-list --left-right --count origin/main...HEAD` | Worktree is clean with `main...origin/main [ahead 1]`; ahead/behind count is `0 1`, so remote workflow and GHCR evidence cannot reflect this v2.2 commit until it is pushed and tagged |
| `git diff --name-only | wc -l`, `git ls-files --others --exclude-standard | wc -l`, and `git diff --cached --name-only | wc -l` | All three counts are `0`; no unstaged, untracked, or staged work remains after the local v2.2 commit |
| `git ls-remote --tags origin 'refs/tags/v2.1.2*' 'refs/tags/v2.2*'` | `refs/tags/v2.1.2-shipping-clean` exists on origin |
| `gh workflow list --repo IMG-LTD/MMMail` | Only `MMMail CI` is listed; no remote `MMMail Images` workflow |
| `gh run list --repo IMG-LTD/MMMail --workflow 'MMMail Images' --limit 5` | Fails with `could not find any workflows named MMMail Images` |
| `gh run view 25959159527 --repo IMG-LTD/MMMail --json jobs` | Latest visible remote `main` run for `e8903bf6` failed in the old frontend and docker baseline jobs |
| `git show e8903bf6:.github/workflows/ci.yml` | Remote `e8903bf6` workflow still contains old Soybean and `frontend-v2` CI jobs; current local workflow is covered separately by root governance contracts |
| `gh api orgs/IMG-LTD/packages/container/mmmail-backend/versions` | Package not found, HTTP 404 |
| `gh api orgs/IMG-LTD/packages/container/mmmail-frontend-admin/versions` | Package not found, HTTP 404 |
| `timeout 60s mvn -f backend/pom.xml -pl mmmail-server -am -Dtest=BackendV22EditionCoreContractTest -Dsurefire.failIfNoSpecifiedTests=false test` | 6 passed, build success |
| `timeout 60s mvn -f backend/pom.xml -pl mmmail-server -am -Dtest=BackendV22CommercialSurfaceCoverageContractTest -Dsurefire.failIfNoSpecifiedTests=false test` | 4 passed, build success |
| `timeout 60s mvn -f backend/pom.xml -pl mmmail-server -am -Dtest=BackendV22EditionCoreContractTest,BackendV22LicenseVerifierContractTest,BackendV22BillingWebhookContractTest,BackendV22EntitlementEnforcementContractTest,BackendV22CommercialSurfaceCoverageContractTest,BackendV22LicenseManagementApiContractTest,BackendV22AuditExportContractTest,BackendV22DsrContractTest,BackendV22OpenTelemetryContractTest,BackendV22OidcSsoContractTest -Dsurefire.failIfNoSpecifiedTests=false test` | 41 passed, build success |
| `bash scripts/validate-v22-external-evidence.sh` | Expected failure; lists the 7 remaining incomplete external evidence markers plus live read-only checks for missing evidence files, unpublished local v2.2 commit state, Images workflow, GHCR packages, and private billing repository access |

## Remaining Evidence Gaps

These items cannot be completed by repository edits alone without live infrastructure, a real tag workflow run, or a private billing repository. Their concrete acceptance criteria are in `docs/v22-external-evidence-checklist.md`:

1. BUS-01 requires live Keycloak login, callback, MMMail session, logout, and token refresh e2e evidence before it can move from partial done to done.
2. OBS-01 requires the live Keycloak route/error trace evidence tied to BUS-01 before it can move from partial done to done.
3. DEP-02 requires a real tag push and resulting image digest evidence before the image-publishing item can move from partial done to done.
4. GATE-01 requires the live Keycloak e2e gate evidence before the release gate expansion can move from partial done to done.
5. Real payment processing, customer portal, invoices/refunds, and license signing private keys remain outside the public repository in the independent billing repository.

## Publication Authorization Blocker

The current v2.2 worktree is clean and committed locally, but it is still unpublished because `main` is ahead of `origin/main` by one commit. The remaining external evidence cannot be produced until an authorized maintainer explicitly allows the publication sequence:

1. Push the local v2.2 commit to a public remote branch.
2. Push the matching release tag.
3. Wait for the remote `MMMail Images` tag workflow and GHCR backend/frontend-admin package digests.
4. Attach live OIDC evidence and private billing evidence packages that reference the same Public MMMail commit.

Until that authorization and external infrastructure exist, the objective must remain `not-complete-external-evidence-required`.

## Completion Decision

The in-repository governance drift found during the audit has been fixed and covered by root governance tests. The overall objective is not complete until the external evidence gaps above are resolved with real evidence.
