---
name: v2.2 external evidence checklist
date: 2026-05-17
source_audit: docs/v22-completion-audit.md
status: pending-external-evidence
---

# v2.2 External Evidence Checklist

This checklist defines the real-world evidence required before the remaining v2.2 items can move from partial/external to done. It prevents proxy completion such as passing local tests, placeholder screenshots, mock payment success, or dry-run image workflows from being treated as final evidence.

## Evidence Rules

- Evidence must come from the real external system named in the row.
- Redact secrets, tokens, private keys, customer data, and full vulnerability details.
- A local contract test can confirm wiring, but it cannot replace live external evidence.
- When an item is completed, update `docs/v22-completion-audit.md`, `docs/v22-open-source-commercial-spec.md`, and the specific source document named below.

## Publication Preconditions

- External evidence cannot be produced from a local uncommitted worktree.
- The v2.2 implementation commit must be present on the remote default branch or release branch before tag-triggered image evidence is accepted.
- The release tag must point to the same commit used by the backend, frontend, OIDC, image digest, and billing evidence packages.
- The remote `.github/workflows/images.yml` must be visible on that commit before running the tag-push image workflow.

## Required External Evidence

| Item | External source | Required evidence | Repository update after evidence exists |
|---|---|---|---|
| BUS-01 live Keycloak SSO | Real Keycloak or approved OIDC IdP | Redacted run record covering login, callback, MMMail session issuance, logout, and token refresh; includes IdP version, callback URL, test user domain, timestamps, backend commit, and relevant trace IDs; use `docs/commercial/oidc-live-evidence-template.md` | `docs/commercial/oidc-sso.md`, `docs/commercial/oidc-live-evidence-template.md`, `docs/security/threat-model.md`, `docs/v22-completion-audit.md`, `docs/v22-open-source-commercial-spec.md`, OIDC e2e gate |
| OBS-01 live OIDC trace evidence | Same live Keycloak/OIDC run as BUS-01 | Redacted trace evidence showing `mmmail.oidc.callback` success and error paths, correlated request IDs, and explicit failure propagation; use `docs/commercial/oidc-live-evidence-template.md` | `docs/observability/opentelemetry.md`, `docs/observability/sli-slo.md`, `docs/commercial/oidc-live-evidence-template.md`, `docs/v22-completion-audit.md`, OTel/OIDC gate |
| GATE-01 live Keycloak e2e gate | Real release or CI gate run against Keycloak/OIDC | Passing gate evidence for the live Keycloak login/callback/session/logout/token-refresh path, tied to the same backend commit and redacted environment metadata as BUS-01; use `docs/commercial/oidc-live-evidence-template.md` | `scripts/release-gate.sh` or dedicated OIDC e2e gate, `docs/commercial/oidc-live-evidence-template.md`, `docs/v22-completion-audit.md`, `docs/v22-open-source-commercial-spec.md`, root governance contract |
| DEP-02 image digest | Real tag push workflow | Successful tag workflow run URL plus immutable digest for backend and frontend-admin images; release notes include both image names and digests; use `docs/release/image-digest-evidence-template.md` | `docs/release/release-notes-template.md` or concrete release notes, `docs/release/image-digest-evidence-template.md`, `docs/v22-completion-audit.md`, image workflow contract |
| Private billing repository | Independent billing repository and payment provider sandbox/live environment | Redacted evidence for provider adapter, webhook delivery, customer portal boundary, invoice/refund handling, and license signing using private signing key outside the public repo; use `docs/billing/private-billing-evidence-template.md` | `docs/commercial/pricing-boundaries.md`, `docs/billing/webhook-signature.md`, `docs/billing/private-billing-evidence-template.md`, `contracts/license/license-claims.schema.json`, `docs/v22-completion-audit.md`, commercial contract docs |

## Completed External Evidence

| Item | Evidence | Repository update |
|---|---|---|
| GitHub private vulnerability reporting | `gh api --method GET repos/IMG-LTD/MMMail/private-vulnerability-reporting` returned `{"enabled":true}` on 2026-05-17 | `SECURITY.md`, `SUPPORT.md`, `docs/v22-completion-audit.md`, root governance contract |

The external evidence verifier rechecks this completed item with the GitHub API each time it runs. If GitHub private vulnerability reporting is disabled or the API call fails, the verifier must fail even before evaluating the remaining pending evidence.

## Non-Evidence

These are useful but not sufficient to mark the external items done:

- `workflow_dispatch` dry runs without tag-published image digests.
- Mock or `none` billing provider events.
- Local OIDC unit tests without a real IdP callback and session lifecycle.
- Screenshots that do not include enough redacted metadata to tie the run to a commit, environment, and timestamp.
- Public security issues are not a replacement for GitHub private vulnerability reporting.

## Completion Rule

Until the required evidence exists, `docs/v22-completion-audit.md` must keep the overall audit status as `not-complete-external-evidence-required`.

`bash scripts/validate-v22-external-evidence.sh` is the explicit verifier for this boundary. In the current repository state it must fail and list the remaining external evidence gaps. It should pass only after the external systems produce real evidence and the documents above are updated.

When the incomplete markers are removed, the verifier must also check real evidence inputs instead of trusting documentation text alone:

- `MMMAIL_OIDC_LIVE_EVIDENCE_FILE` must point to the redacted live Keycloak/OIDC evidence package and include `mmmail.oidc.callback`.
- `MMMAIL_IMAGE_DIGEST_EVIDENCE_FILE` must point to the image digest evidence package and include immutable `sha256:` digests, `Workflow event: push`, and `Workflow conclusion: success`.
- `MMMAIL_PRIVATE_BILLING_EVIDENCE_FILE` must point to the private billing evidence package and include license signing evidence.
- Each evidence package must contain `Evidence status: completed-external-evidence`; the template files themselves and unfilled template copies are rejected.
- The required metadata fields in each evidence package must be filled with non-empty values, including commit SHAs, provider/run metadata, workflow URL, immutable digests, and billing repository details.
- Image digest evidence must contain real immutable digests, not the `sha256:*` wildcard placeholder.
- GitHub CLI checks must see a successful `push` event run for the `MMMail Images` workflow, GHCR backend/frontend-admin package versions, and an accessible `IMG-LTD/mmmail-billing-gateway` repository. GHCR package version checks require a `gh` token with `read:packages`; a GitHub API 403 is a permissions evidence gap, not proof that the package does not exist.
- The completed evidence files must point to the same Public MMMail commit: OIDC backend commit, OIDC frontend commit, image digest commit, and private billing evidence `Public MMMail repository commit SHA` must match.
- The release tag named by the image digest evidence must be visible on `origin`, must resolve to that same commit, and that commit must be contained in `origin/main` or `origin/release/*`.

Because this verifier is expected to fail until external systems are complete, it must not be executed by default `scripts/validate-local.sh`, CI, or release-gate paths while `audit_status` remains `not-complete-external-evidence-required`. Those green gates may require the verifier file to exist, but they must not run it as a passing proxy.

Default root governance tests may statically assert this verifier's required sources, manual-only wiring, and expected failure markers, but must not execute `bash scripts/validate-v22-external-evidence.sh`. Live execution belongs to manual external evidence acceptance and must not make the normal local/CI green path depend on GitHub CLI state, GHCR visibility, remote workflow publication, or private repository access.

## Current Incomplete Markers

The verifier must currently report these incomplete markers:

- `completion audit is still marked not-complete-external-evidence-required`
- `external evidence checklist is still pending`
- `BUS-01 live Keycloak SSO remains partial`
- `DEP-02 image digest evidence remains partial`
- `OBS-01 live OIDC trace evidence remains partial`
- `GATE-01 live Keycloak e2e gate remains partial`
- `private billing repository and real payment evidence remain external`

It also reports current read-only evidence gaps that can be evaluated before the status markers are removed:

- `live OIDC evidence file is not provided`
- `image digest evidence file is not provided`
- `private billing evidence file is not provided`
- `backend GHCR package versions are not visible`
- `frontend-admin GHCR package versions are not visible`
- `private billing repository is not accessible`

If the GHCR rows fail with HTTP 403 or GitHub's `read:packages` message, the missing item remains visible until package metadata can be read with an appropriately scoped token. The verifier still retains publication precondition and workflow visibility checks for regressions, but rc10 already provides successful `MMMail Images` workflow visibility for the current main-repo commit.
