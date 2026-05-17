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
| Generated type and diff hygiene | `frontend-admin/package.json`, `frontend-admin/scripts/gen-api.mjs`, `frontend-admin/scripts/normalize-generated-types.mjs`, `scripts/validate-local.sh` | `gen-route` and local validation normalize `elegant-router.d.ts` after route generation; `gen:api` runs `openapi-typescript` and then `oxfmt` in the same script, so generated OpenAPI types match the CI clean-diff guard without manual formatter steps; `validate-local` ends with `git diff --check` so generated trailing whitespace cannot hide behind a green gate | Done |
| Frontend build env defaults | `frontend-admin/.env`, `frontend-admin/.env.test`, `.gitignore`, `.dockerignore`, `AGENTS.md` | `frontend-admin/.env` and `.env.test` are tracked non-secret Vite defaults for CI and Docker fresh builds; root/local secret env files remain ignored, and `.dockerignore` excludes only root `.env*` files rather than frontend-admin defaults | Done |
| Frontend e2e Java toolchain | `.github/workflows/ci.yml`, `AGENTS.md`, `tests/v22-ci-toolchain-contract.test.mjs` | The frontend CI job installs Temurin Java 21 before `pnpm --dir frontend-admin test:e2e`, because that browser gate starts Docker-backed backend setup and compiles Maven modules with `--release 21` | Done |
| Frontend Lighthouse and auth first screen | `frontend-admin/package.json`, `frontend-admin/pnpm-lock.yaml`, `frontend-admin/scripts/run-lighthouse.mjs`, `frontend-admin/src/views/_builtin/login/modules/pwd-login.vue`, `frontend-admin/tests/v212-browser-gates-contract.test.mjs`, `tests/v22-ci-toolchain-contract.test.mjs` | `scripts/run-lighthouse.mjs` directly imports `chrome-launcher`, so the package declares `chrome-launcher@1.2.1` as a direct devDependency; the script explicitly uses Lighthouse `preset: 'desktop'` while keeping the `> 80` threshold, and password login first screen now uses native form controls instead of Naive form components so the CI runner has real performance headroom | Done |
| Backend dependency security baseline | `backend/pom.xml`, `DependencyVersionGuardTest`, `config/dependency-check-suppressions.xml`, `tests/v22-ci-toolchain-contract.test.mjs` | Remote rc5 validate exposed real OWASP dependency-check failures for high-risk runtime dependencies, fixed by pinned patched baselines; remote rc8 then exposed a CPE false positive mapping Java `opentelemetry-semconv@1.41.1` to OpenTelemetry-Go CVEs, now handled by an exact package URL + CVE suppression while keeping `failBuildOnCVSS=7` active | Done |
| Security disclosure routing / GitHub private vulnerability reporting | `SECURITY.md`, `SUPPORT.md`, `.github/ISSUE_TEMPLATE/security-contact-request.md` | `gh api --method GET repos/IMG-LTD/MMMail/private-vulnerability-reporting` returned `{"enabled":true}`; root governance contract checks private reporting wording and fallback route | Done |
| Repository governance drift check | AGENTS, spec, root governance contracts, validate-local required files | `node --test tests/v22-repository-governance-contract.test.mjs tests/v22-repository-governance-validation-contract.test.mjs`; root contracts now fix the frozen `frontend-v2` wording in README, install docs, topology docs, CONTRIBUTING, PR template and CODEOWNERS; backend norms require explicit runtime constructor injection for multi-constructor Spring beans; active source line limits use an explicit legacy allowlist; local agent / validation artifacts stay ignored; root contract gate naming no longer claims v2.1.2-only coverage | Done |
| External evidence acceptance | `docs/v22-external-evidence-checklist.md`, `scripts/validate-v22-external-evidence.sh` | Root governance contract checks required external sources, non-evidence examples, expected verifier failure markers, and that default green gates do not run the failing verifier directly or indirectly through root tests; the verifier rechecks GitHub private vulnerability reporting via GitHub API, reports current read-only evidence gaps, and also requires completed evidence files with non-empty metadata fields, matching Public MMMail commit SHAs, an origin release tag pointing to that commit, remote branch containment, not templates, plus successful tag-push image workflow, GHCR package, and billing repo checks after incomplete markers are removed | Pending external evidence |

Verifier completed-state acceptance requires completed evidence files with non-empty metadata fields, not templates. It also requires those files to agree on the same Public MMMail commit, origin release tag commit, and remote branch containment.

## Verified Commands

The latest targeted verification for this audit:

| Command | Result |
|---|---|
| `node --test tests/v22-ci-toolchain-contract.test.mjs tests/v22-repository-governance-contract.test.mjs tests/v22-repository-governance-validation-contract.test.mjs` | 32 passed after pass-90 security report path governance update |
| `timeout 60s mvn -f backend/pom.xml -pl mmmail-server -am -Dtest=DependencyVersionGuardTest -Dsurefire.failIfNoSpecifiedTests=false test` | 21 passed; confirms Spring Boot 3.5.14, Spring Security 6.5.10, Tomcat 10.1.55, Log4j 2.26.0, Netty 4.1.133.Final, OpenTelemetry semconv 1.41.1, Kotlin stdlib 2.3.21, Swagger UI 5.32.5, and the existing dependency guard surface |
| `timeout 900s mvn -f backend/pom.xml org.owasp:dependency-check-maven:aggregate -Dformats=HTML,JSON -Dodc.outputDirectory=/tmp/mmmail-security-path-check/dependency-check -DdataDirectory=.tools/dependency-check-data -DfailBuildOnCVSS=7 -DsuppressionFile=config/dependency-check-suppressions.xml -DautoUpdate=false` | Build success using the cached dependency-check database and the explicit OpenTelemetry-Go false-positive suppression; HTML and JSON reports were written to `/tmp/mmmail-security-path-check/dependency-check/`, proving the validator can require repository-root anchored report paths |
| `node --test tests/v22-dsr-inventory-contract.test.mjs tests/v22-repository-governance-contract.test.mjs tests/v22-repository-governance-validation-contract.test.mjs` | 24 passed |
| `MMMAIL_OPENAPI_SOURCE=../contracts/openapi/v21-api-catalog.yaml pnpm --dir frontend-admin gen:api && git diff --exit-code -- frontend-admin/src/service/api/__generated__` | Passed; `openapi-typescript` generated `openapi.d.ts`, `oxfmt` formatted it, and the generated API directory kept a clean diff |
| `node --test tests/v22-ci-toolchain-contract.test.mjs tests/v212-shipping-cleanup-contract.test.mjs tests/v212-coverage-gates-contract.test.mjs tests/v212-module-design-docs-contract.test.mjs` | 22 passed before pass-83; covers tracked v2.1.2 fixture docs, no runner `rg` dependency, pnpm engine-compatible CI setup, action major upgrades, direct `@iconify/utils` dependency and Docker context exclusions |
| `node --test tests/*.test.mjs` | 90 passed after pass-83 governance, CI toolchain, API-generation and frontend env additions |
| `pnpm --dir frontend-admin test:v212` | 125 passed, including v2.2 commercial license, billing, OIDC entry, entitlement localization, commercial a11y gate contracts, the explicit Lighthouse desktop preset contract, and the password-login native first-screen contract |
| `sg docker -c "cd /home/xiang/桌面/project/MMMail-test/MMMail && env -u http_proxy -u https_proxy -u HTTP_PROXY -u HTTPS_PROXY -u ALL_PROXY -u all_proxy pnpm --dir frontend-admin test:e2e"` | 11 passed; confirms backend Spring startup after the OIDC constructor fix |
| `pnpm --dir frontend-admin test:lighthouse` | Passed after a fresh build, direct `chrome-launcher` dependency declaration, explicit Lighthouse desktop preset, and native password-login first-screen controls; `/login` Lighthouse performance=86 |
| `sg docker -c "cd /home/xiang/桌面/project/MMMail-test/MMMail && bash scripts/validate-batch3.sh"` | 9 passed, build success after V39 release metadata update |
| `sg docker -c "PATH=/tmp/mmmail-helm-bin:$PATH env -u http_proxy -u https_proxy -u HTTP_PROXY -u HTTPS_PROXY -u ALL_PROXY -u all_proxy bash scripts/validate-local.sh"` | `all checks passed` after the pass-77 repository governance changes; includes root repository contract gates, legacy frontend freeze, split route/locale/limiter governance, active source line-limit allowlist, generated type hygiene and `git diff --check`; confirms default local validation does not execute the manual-only external evidence verifier; local shell required Docker group refresh via `sg docker` and temporary Helm 3.14.4 on `PATH` |
| `pnpm --dir frontend-admin build` | Build successful after declaring `@iconify/utils` as a direct devDependency and tracking frontend-admin Vite env defaults |
| `pnpm --dir frontend-admin typecheck` | Passed |
| `pnpm --dir frontend-admin exec oxlint`, `pnpm --dir frontend-admin exec eslint .`, `pnpm --dir frontend-admin exec oxfmt --check` | `oxlint` found 0 warnings / 0 errors; ESLint exited 0; `oxfmt --check` reported all matched files use the correct format |
| `sg docker -c 'cd /home/xiang/桌面/project/MMMail-test/MMMail && docker build --no-cache --platform linux/amd64 -f frontend-admin/Dockerfile -t mmmail-frontend-admin:ci-env-contract .'` | Frontend-admin Docker no-cache build passed; the context transfer reported `212.84kB`, fresh install included `@iconify/utils`, and `pnpm build` completed inside the image build with tracked Vite env defaults |
| `git commit -s -m "feat(v22): implement open source commercial readiness"` and `git push origin main && git push origin v2.2.0-rc.1` | Published DCO-signed commit `f29a151054be4fbceaf1d72b938fb1fba8449487` to `origin/main`; annotated tag `v2.2.0-rc.1` was pushed and points to that commit |
| `git diff --check` | Passed after generated type hygiene fix and pass-77 governance edits |
| `bash -n scripts/validate-local.sh scripts/validate-v22-external-evidence.sh` | Passed |
| `bash scripts/validate-v22-external-evidence.sh` | Failed as required while external evidence is incomplete; earlier pre-publication output included the unpublished-commit gap; after publishing, rc1 still cannot satisfy DEP-02 because the tag workflow failed for frontend-admin image publishing |
| `rg -n "[ \t]+$" frontend-admin/src/typings/elegant-router.d.ts AGENTS.md README.md CONTRIBUTING.md DCO.md .github/pull_request_template.md docs/frontend/v22-frontend-topology-audit.md docs/frontend/v22-frontend-convergence-decision.md docs/v22-open-source-commercial-spec.md docs/v22-completion-audit.md docs/v22-external-evidence-checklist.md scripts/validate-local.sh scripts/validate-v22-external-evidence.sh tests/v22-dsr-inventory-contract.test.mjs tests/v22-repository-governance-contract.test.mjs tests/v22-repository-governance-validation-contract.test.mjs` | No trailing whitespace matches |
| Final newline check for the same touched governance files | Passed |
| `wc -l scripts/validate-v22-external-evidence.sh tests/v22-repository-governance-contract.test.mjs tests/v22-repository-governance-validation-contract.test.mjs tests/v22-ci-toolchain-contract.test.mjs DCO.md` | External verifier is 426 lines; governance test files are 471, 450 and 96 lines; `DCO.md` is 36 lines |
| `gh api --method GET repos/IMG-LTD/MMMail/private-vulnerability-reporting` | `{"enabled":true}` |
| `gh repo view IMG-LTD/mmmail-billing-gateway --json name,owner,visibility,isPrivate,defaultBranchRef,url` | Not found / not accessible; org repo list currently does not include `mmmail-billing-gateway` |
| `docker ps --format '{{.Names}}\t{{.Image}}\t{{.Ports}}'` | Docker daemon access denied in the normal shell |
| `sg docker -c "docker ps --format '{{.Names}}\t{{.Image}}\t{{.Ports}}'"` | Only `mmmail-nacos` and `mmmail-kafka` are running; no Keycloak/OIDC container evidence |
| `sg docker -c "docker compose ps --format json"` | Compose emitted missing-env warnings and did not produce live Keycloak/OIDC service evidence |
| `env | cut -d= -f1 | rg -i 'MMMAIL_OIDC|OIDC|KEYCLOAK|OTEL|MMMAIL_BILLING|MMMAIL_LICENSE'` | No matching environment variable names in the current shell |
| `ss -ltnp | rg '(:8080|:8081|:5173|:3000|:8443|:9000|:18080|:3306|:6379|:9725|:19527)'` | Only Java on `:8080` was visible; no Keycloak/OIDC listener evidence |
| `git ls-remote --tags origin 'refs/tags/v2.1.2*' 'refs/tags/v2.2*'` | `refs/tags/v2.1.2-shipping-clean` and `refs/tags/v2.2.0-rc.1` exist on origin |
| `gh workflow list --repo IMG-LTD/MMMail` | `MMMail CI`, `DCO`, `MMMail Images`, and Dependabot workflows are visible after the v2.2 workflow push |
| `gh run view 25975040608 --repo IMG-LTD/MMMail --json status,conclusion,jobs,url,headSha,headBranch,event,displayTitle` | Remote `main` CI for `f29a1510` completed with failure: root contracts failed because required `docs/superpowers/specs/2026-05-15-*` fixture documents were not tracked and the shipping cleanup contract called `rg` on a runner without that binary; docker baseline failed because workflow pnpm was 9 while `frontend-admin` requires pnpm `>=10.5.0` |
| `gh run view 25975044095 --repo IMG-LTD/MMMail --json status,conclusion,jobs,url,headSha,headBranch,event,displayTitle` | Tag workflow `v2.2.0-rc.1` completed with failure: backend image succeeded, frontend-admin image failed during `pnpm build` |
| `gh api repos/IMG-LTD/MMMail/actions/jobs/76353625731/logs` | Frontend-admin Docker build failed with `ERR_MODULE_NOT_FOUND` for `@iconify/utils`, because `build/plugins/unocss.ts` imported it directly while `frontend-admin/package.json` did not declare it as a direct dependency |
| `gh api orgs/IMG-LTD/packages/container/mmmail-backend/versions` | Package not found, HTTP 404 |
| `gh api orgs/IMG-LTD/packages/container/mmmail-frontend-admin/versions` | Package not found, HTTP 404 |
| `timeout 60s mvn -f backend/pom.xml -pl mmmail-server -am -Dtest=BackendV22EditionCoreContractTest -Dsurefire.failIfNoSpecifiedTests=false test` | 6 passed, build success |
| `timeout 60s mvn -f backend/pom.xml -pl mmmail-server -am -Dtest=BackendV22CommercialSurfaceCoverageContractTest -Dsurefire.failIfNoSpecifiedTests=false test` | 4 passed, build success |
| `timeout 60s mvn -f backend/pom.xml -pl mmmail-server -am -Dtest=BackendV22EditionCoreContractTest,BackendV22LicenseVerifierContractTest,BackendV22BillingWebhookContractTest,BackendV22EntitlementEnforcementContractTest,BackendV22CommercialSurfaceCoverageContractTest,BackendV22LicenseManagementApiContractTest,BackendV22AuditExportContractTest,BackendV22DsrContractTest,BackendV22OpenTelemetryContractTest,BackendV22OidcSsoContractTest -Dsurefire.failIfNoSpecifiedTests=false test` | 41 passed, build success |
| `bash scripts/validate-v22-external-evidence.sh` | Expected failure while external evidence remains incomplete; after rc10, the image workflow has succeeded but completed image digest evidence files, visible GHCR package metadata and private billing evidence are still unavailable |
| `gh run view 25977701508 --repo IMG-LTD/MMMail --json status,conclusion,jobs,url,headSha,headBranch,event,displayTitle` | Remote `MMMail CI` for commit `50165923` / tag `v2.2.0-rc.10` completed with success across backend, frontend, Docker baseline, validate and release-gate jobs |
| `gh run view 25977702756 --repo IMG-LTD/MMMail --json status,conclusion,jobs,url,headSha,headBranch,event,displayTitle` | Remote `MMMail Images` for commit `50165923` / tag `v2.2.0-rc.10` completed with success for backend and frontend-admin image jobs |
| `timeout 120s bash scripts/validate-v22-external-evidence.sh` | Expected failure with status 1: completion audit/checklist still mark external evidence incomplete; live OIDC evidence file, image digest evidence file and private billing evidence file are missing; backend and frontend-admin GHCR package versions are not visible to the verifier; private billing repository is not accessible |

## Remaining Evidence Gaps

These items cannot be completed by repository edits alone without live infrastructure, completed evidence files, visible GHCR package / digest metadata, or a private billing repository. Their concrete acceptance criteria are in `docs/v22-external-evidence-checklist.md`:

1. BUS-01 requires live Keycloak login, callback, MMMail session, logout, and token refresh e2e evidence before it can move from partial done to done.
2. OBS-01 requires the live Keycloak route/error trace evidence tied to BUS-01 before it can move from partial done to done.
3. DEP-02 has a successful `v2.2.0-rc.10` Images workflow, but still requires completed image digest evidence, visible GHCR package / digest evidence, and release note recording before the image-publishing item can move from partial done to done.
4. GATE-01 requires the live Keycloak e2e gate evidence before the release gate expansion can move from partial done to done.
5. Real payment processing, customer portal, invoices/refunds, and license signing private keys remain outside the public repository in the independent billing repository.

## Remote Publication Status

The v2.2 main-repo implementation was first published as commit `f29a151054be4fbceaf1d72b938fb1fba8449487` on `origin/main`, and tag `v2.2.0-rc.1` was pushed. That rc1 is not acceptable release evidence because the remote gates exposed repository-governance drift:

1. CI used pnpm 9 while the product frontend requires pnpm `>=10.5.0`.
2. Root contracts depended on three untracked `docs/superpowers/specs/2026-05-15-*` fixture documents.
3. One root contract depended on an undeclared `rg` runner binary.
4. Frontend image publishing depended on a transitive `@iconify/utils` package that the Vite config imports directly.

The follow-up commit `f00b2a50a564c686cab800296a930ec7ff3f8599` and tag `v2.2.0-rc.2` fixed those rc1 defects, but rc2 is still not acceptable release evidence because the fresh remote gates exposed two additional repository-governance gaps:

1. CI `MMMail admin API type generation guard` ran `gen:api` and then detected generated OpenAPI type formatting drift, because `frontend-admin/scripts/gen-api.mjs` previously ran `openapi-typescript` without the project formatter.
2. Frontend-admin image publishing failed Vite config loading with `Cannot read properties of undefined (reading 'replace')`, because `VITE_ICON_LOCAL_PREFIX` lived only in untracked `frontend-admin/.env` and `.dockerignore` excluded env files from the fresh Docker context.

The follow-up commit `12220877ab02c2d07996e3569c49d3b3387a0f5f` and tag `v2.2.0-rc.3` fixed the rc2 API-generation and frontend image env failures. rc3 is still not acceptable release evidence because remote CI then exposed a frontend job toolchain gap: `pnpm --dir frontend-admin test:e2e` invokes Docker-backed backend setup, and Maven failed with `Fatal error compiling: error: release version 21 not supported` because the frontend CI job had not installed Java 21.

The follow-up commit `3b65e990b91ba7eefd8f1841d0ac51d907cd251d` and tag `v2.2.0-rc.4` fixed the Java 21 e2e toolchain gap. rc4 is still not acceptable release evidence because remote CI then exposed one more direct-dependency gap: `scripts/run-lighthouse.mjs` imports `chrome-launcher`, but `frontend-admin/package.json` had not declared it directly, so `pnpm --dir frontend-admin test:lighthouse` failed with `ERR_MODULE_NOT_FOUND`.

The follow-up commit `5b3c68576a9076c707cbd157498ca401dd2988d1` and tag `v2.2.0-rc.5` fixed the Lighthouse direct-dependency gap. rc5 is still not acceptable release evidence: `MMMail Images` succeeded for backend and frontend-admin, and CI backend, frontend, Docker baseline, and release-gate jobs succeeded, but the CI validate job failed in OWASP dependency-check. The failure list included high-risk backend dependencies such as `log4j-api`, `opentelemetry-semconv`, `spring-boot`, and `tomcat-embed-core`; the report also highlighted related Spring Security, Kotlin stdlib, and Swagger UI surface that must stay on patched lines.

The follow-up commit `0e6dda424db3004334fb7ca09199f2e41f97f6d1` and tag `v2.2.0-rc.6` fixed the backend dependency security baseline. rc6 is still not acceptable release evidence because the CI frontend job failed in `pnpm --dir frontend-admin test:lighthouse`: `/login` produced Lighthouse performance `80`, and the script requires a score greater than `80`. Backend and Docker baseline jobs succeeded, but validate and release-gate were skipped after the frontend failure. The root issue is that `run-lighthouse.mjs` did not explicitly use the historical desktop Lighthouse preset, so CI measured the page at the threshold boundary.

The follow-up commit `4f61d48f3baea604c15e36be3840c7637ac6c0dc` and tag `v2.2.0-rc.7` fixed the Lighthouse measurement preset. rc7 is still not acceptable release evidence because remote CI again measured `/login` at Lighthouse performance `80` under the desktop preset. Backend and Docker baseline jobs succeeded, but validate and release-gate were skipped after the frontend failure. The root issue is now insufficient first-screen performance headroom: password login still loaded Naive form components on the default login route, so the implementation was valid functionally but too close to the `> 80` gate in GitHub runner conditions.

The follow-up commit `61293d4493294801368a7bb63a9353a9f5a7a373` and tag `v2.2.0-rc.8` fixed the login first-screen performance headroom. rc8 is still not acceptable release evidence because the remote validate job failed only in OWASP dependency-check: `io.opentelemetry.semconv:opentelemetry-semconv@1.41.1` was matched to OpenTelemetry-Go CVEs `CVE-2026-39882` and `CVE-2026-39883` through CPE metadata. Maven Central currently lists `1.41.1` as the latest release for the Java artifact, so the fix is an explicit narrow suppression tied to that package URL and those CVEs, not a disabled scan or lower CVSS gate.

The follow-up commit `921108688aa2e3b9f0a08bfba05d0555e1bd2489` and tag `v2.2.0-rc.9` fixed the OpenTelemetry dependency-check false positive. rc9 is still not acceptable release evidence because remote dependency-check then reached `BUILD SUCCESS`, but `validate-security.sh` could not find the generated HTML/JSON reports. The root cause is path resolution: CI sets `MMMAIL_SECURITY_REPORT_DIR=artifacts/security`, and Maven executed with `-f backend/pom.xml` wrote that relative path under `backend/artifacts/security` while the validator checked `artifacts/security` at the repository root. Security report paths now normalize relative env values to `$ROOT_DIR/...` before invoking Maven and before checking required reports.

The follow-up commit `50165923b6dddc6c2f9d96dc5ab7bb2c8b47a2d4` and tag `v2.2.0-rc.10` fixed the security report path issue. `MMMail CI` run `25977701508` and `MMMail Images` run `25977702756` both completed with success for that commit; rc10 provides current main-repo remote release evidence for the repository gates and image workflow.

That rc10 evidence still does not complete v2.2 external acceptance. The manual external verifier continues to fail because live OIDC evidence, image digest evidence files, private billing evidence, GHCR package visibility and the private billing repository are still unavailable in the current environment.

## Completion Decision

The in-repository governance drift found during the audit has been fixed and covered by root governance tests. The overall objective is not complete until the external evidence gaps above are resolved with real evidence.
