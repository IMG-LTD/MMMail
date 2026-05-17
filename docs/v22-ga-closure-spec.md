---
name: v2.2 GA closure spec
date: 2026-05-17
spec_version: ga-v3.0
based_on:
  - docs/v22-open-source-commercial-spec.md (oss-comm-v1.97)
  - docs/v22-completion-audit.md (2026-05-17, current published head d6921aa2)
  - docs/v22-external-evidence-checklist.md (2026-05-17)
  - scripts/validate-v22-external-evidence.sh (full external evidence verifier)
  - CI run 25983793250 (success, headSha d6921aa2)
status: ready-for-public-release-closure
release_gate: v2.2 public release closure / full GA pending external evidence
target_audience: maintainer / external reviewer / CI runner
---

# v2.2 GA Closure Spec

This document defines how to close v2.2 without weakening the existing evidence model.

It replaces the earlier draft that tried to make `private billing repository is not accessible` a non-blocking verifier residual. That approach is not valid for the current repository because `scripts/validate-v22-external-evidence.sh` is the **full external evidence verifier**. Its meaning must stay strict: it passes only when OIDC, image digest, release notes, GHCR visibility, and private billing evidence are all real, completed, and bound to the same Public MMMail commit.

## 0. Closure Levels

| Level | Purpose | Release label | Full external verifier | Billing evidence |
|---|---|---|---|---|
| Public release closure | Publish a clearly scoped public v2.2 artifact after main-repo gates are green and public release evidence is present | `v2.2.0-public` | Expected to fail while private billing remains external | Must remain marked external/deferred, never completed |
| External evidence closure | Attach real live OIDC, image digest, release notes, GHCR visibility, and private billing evidence | no separate release required | Must pass with exit code 0 | Required |
| Full GA closure | Promote to full `v2.2.0` after all external evidence is complete | `v2.2.0` | Must pass with exit code 0 | Required |

### 0.1 Non-Negotiable Rules

- Do not change `scripts/validate-v22-external-evidence.sh` to skip private billing for GA.
- Do not introduce `MMMAIL_SKIP_PRIVATE_BILLING_CHECK` or any similar bypass into the full verifier.
- Do not mark private billing as `completed-external-evidence` unless the private repository, provider adapter, webhook delivery, customer portal boundary, invoice/refund flow, and license signing evidence are all real and accessible to the acceptance operator.
- Do not mark `docs/v22-completion-audit.md` as `complete` or `complete-public-ga` while the full external verifier still fails.
- Do not copy template files into evidence files and fill them with placeholders.
- Do not treat `workflow_dispatch`, local Docker builds, screenshots without trace metadata, GHCR 403 responses, or an inaccessible private repo as completion evidence.

### 0.2 Current Facts

| Item | Current value |
|---|---|
| Current published head | `d6921aa23e601c107f4b992fa7039a6a7a866b62` (`d6921aa2`) |
| Latest known main CI | `MMMail CI` run `25983793250`, success |
| Historical image baselines | `v2.2.0-rc.10`, `v2.2.0-rc.11`, `v2.2.0-rc.13` tag-push image workflows succeeded |
| Latest visible GitHub Release | `v2.0.4`; no v2.2 release notes are visible yet |
| Current GitHub CLI scopes | `gist`, `read:org`, `repo`; missing `read:packages` |
| Completion audit status | `not-complete-external-evidence-required` |
| External evidence checklist status | `pending-external-evidence` |
| Full external verifier current state | expected failure while external evidence is incomplete |
| This spec file state before adoption | untracked until explicitly added and committed |

Historical image workflow success proves that the workflow can publish images. It does **not** replace final image digest evidence for the acceptance commit.

### 0.3 Same-Commit Binding

All completed evidence for full GA must bind to the same Public MMMail commit:

| Evidence | Required binding |
|---|---|
| OIDC backend commit SHA | Same Public MMMail commit |
| OIDC frontend commit SHA | Same Public MMMail commit |
| Image digest evidence commit SHA | Same Public MMMail commit |
| Private billing evidence `Public MMMail repository commit SHA` | Same Public MMMail commit |
| Release tag | Points to the same commit |
| GitHub Release notes | Published for the same tag |
| Remote branch containment | Commit is contained in `origin/main` or `origin/release/*` |

For public release closure, the release tag points to the CI-green acceptance implementation commit. Evidence documents may be committed after the tag-triggered workflow produces digests, but their internal metadata must reference that tag commit, and the evidence commit must be pushed before public closure is declared.

## 1. What Public Release Closure May Claim

Public release closure may claim:

- Main-repository code, governance, frontend convergence, commercial boundaries, Helm, Audit/DSR, OIDC baseline, OpenTelemetry baseline, and the 17-step release gate are green.
- Container images are published for the public release tag with immutable digests and GitHub Release notes.
- Live OIDC evidence for the public release tag is attached if it was actually produced.

Public release closure must not claim:

- Full GA.
- Real payment readiness.
- Private billing implementation completion.
- License signing private-key flow completion.
- `scripts/validate-v22-external-evidence.sh` success if that script still fails.

Release notes for `v2.2.0-public` must explicitly say:

> This is a public release closure, not full GA. Full GA requires `scripts/validate-v22-external-evidence.sh` to pass with real private billing evidence.

## 2. Remaining Work By Closure Level

| Work item | Public release closure | Full GA closure |
|---|---|---|
| Current main CI success | Required | Required |
| `read:packages` GHCR visibility | Required if publishing image digest claims | Required |
| Public release tag | Required | Required |
| GitHub Release notes with image digests | Required | Required |
| Image digest evidence file | Required | Required |
| Live OIDC evidence file | Required if claiming live OIDC verification | Required |
| Private billing evidence file | Must remain external/deferred unless real evidence exists | Required |
| Private billing repository access | Not claimed | Required |
| Full external verifier exit code 0 | Not expected until private billing is complete | Required |

## 3. Public Release Closure Procedure

### P-0: Adopt This Spec

Commit this spec before running release commands.

```bash
git add docs/v22-ga-closure-spec.md
git commit -m "docs(v22): define ga closure evidence model"
git push origin main
```

After push, wait for `MMMail CI` to complete successfully on the new commit. The release commit used below must be this published, CI-green commit or a later CI-green commit.

### P-1: Refresh GHCR Read Permission

GHCR package metadata is part of the public image evidence. Current CLI auth lacks `read:packages`.

```bash
gh auth refresh -h github.com -s read:packages
gh auth status
gh api /orgs/IMG-LTD/packages/container/mmmail-backend/versions --jq '.[0].metadata.container.tags'
gh api /orgs/IMG-LTD/packages/container/mmmail-frontend-admin/versions --jq '.[0].metadata.container.tags'
```

Both package version queries must return non-empty metadata. HTTP 403 remains a permission evidence gap.

### P-2: Select The Public Acceptance Commit

Choose the commit that will be released. It must already be published and green in `MMMail CI`.

Do not tag `d6921aa2` blindly if a newer evidence-model commit has been published and is the intended release base. The release base is whichever published commit has the current GA closure model and a successful CI run.

Verify the worktree is clean and synchronised with the remote before tagging. A count like `git rev-list --count origin/main..HEAD` alone only counts commits ahead of `origin/main`; it must be combined with a fresh fetch and a bidirectional count, and the local `origin/main` ref must already be up to date:

```bash
git fetch origin --tags --prune
git status --short --branch
git rev-list --left-right --count origin/main...HEAD
git ls-remote --tags origin refs/tags/v2.2.0-public
```

Proceed only when:

- the worktree is clean (no tracked diffs and no untracked files relevant to this release),
- `git rev-list --left-right --count origin/main...HEAD` reports `0\t0` (HEAD is neither ahead of nor behind `origin/main`),
- the latest `MMMail CI` run for `HEAD` succeeded,
- `v2.2.0-public` does not already exist, or an explicit replacement plan has been approved.

Record the chosen commit:

```bash
PUBLIC_RELEASE_COMMIT="$(git rev-parse HEAD)"
```

### P-3: Publish Public Tag

Then:

```bash
git tag -a v2.2.0-public -m "MMMail v2.2.0-public - public release closure"
git push origin v2.2.0-public
```

### P-4: Wait For Tag-Triggered Images Workflow

```bash
gh run list --repo IMG-LTD/MMMail --workflow "MMMail Images" --event push --limit 5 \
  --json databaseId,status,conclusion,headSha,headBranch,displayTitle,url
```

The accepted run must:

- be a `push` event,
- be triggered by `v2.2.0-public`,
- complete with `success`,
- use the same commit as the tag.

Extract backend and frontend immutable digests from the workflow output or GHCR package metadata.

### P-5: Commit Public Evidence Documents

Create only evidence and status files that are true for the public release scope.

Required:

- `docs/release/image-digest-evidence.md` built from `docs/release/image-digest-evidence-template.md`. The verifier (`scripts/validate-v22-external-evidence.sh`) requires the file to contain, with these exact labels and non-empty values:
  - `Evidence status: completed-external-evidence`
  - `Release tag: v2.2.0-public`
  - `Git commit SHA: <PUBLIC_RELEASE_COMMIT>` (40-char lowercase hex)
  - `GitHub workflow run URL: <MMMail Images run URL>`
  - `GitHub release URL: <https://github.com/IMG-LTD/MMMail/releases/tag/v2.2.0-public>`
  - `Backend immutable digest: sha256:<…>` and `Frontend immutable digest: sha256:<…>` (no `sha256:*` wildcard placeholder)
  - the literal strings `Workflow event: push` and `Workflow conclusion: success`
- Public release notes content with both image names (`mmmail-backend`, `mmmail-frontend-admin`) and both immutable digests; these must match the evidence file byte-for-byte.

Optional but only if real:

- `docs/commercial/oidc-live-evidence.md` if a real Keycloak or approved OIDC IdP run was completed for `PUBLIC_RELEASE_COMMIT`.

Do not create `docs/billing/private-billing-evidence.md` as completed evidence unless private billing is truly complete. If documenting the deferral, use wording such as `Evidence status: deferred-to-full-ga`, not `completed-external-evidence`.

Commit and push the evidence documents:

```bash
git add docs/release/image-digest-evidence.md
git commit -m "docs(v22): record public release image evidence"
git push origin main
```

Wait for `MMMail CI` on that evidence commit. This CI run proves the repository documentation state is green; the release tag remains bound to `PUBLIC_RELEASE_COMMIT`.

### P-6: Create GitHub Release Notes

Release notes must include the exact image names and immutable digests:

```text
ghcr.io/img-ltd/mmmail-backend@sha256:<backend-digest>
ghcr.io/img-ltd/mmmail-frontend-admin@sha256:<frontend-digest>
```

They must also include:

- the release commit SHA,
- the tag name,
- the `MMMail Images` workflow run URL,
- a clear statement that private billing remains outside public release closure unless full billing evidence is attached.

Create the release only after the digest evidence file is committed and pushed:

```bash
gh release create v2.2.0-public \
  --title "MMMail v2.2.0-public - Public Release Closure" \
  --notes-file /tmp/v22-public-release-notes.md
```

Verify:

```bash
gh release view v2.2.0-public --json tagName,name,body
```

### P-7: Public Closure Verification

Run these commands and record results in `docs/v22-completion-audit.md` without changing the full audit status to complete:

```bash
git status --short --branch
gh run view <main-ci-run-id> --repo IMG-LTD/MMMail --json status,conclusion,headSha,url
gh run view <images-run-id> --repo IMG-LTD/MMMail --json status,conclusion,headSha,event,url
gh release view v2.2.0-public --repo IMG-LTD/MMMail --json tagName,name,body
gh api /orgs/IMG-LTD/packages/container/mmmail-backend/versions --jq '.[0].metadata.container.tags'
gh api /orgs/IMG-LTD/packages/container/mmmail-frontend-admin/versions --jq '.[0].metadata.container.tags'
timeout 120s bash scripts/validate-v22-external-evidence.sh
```

The final verifier is expected to fail until full external evidence exists. Its failure output must still be recorded because it proves public closure has not been misrepresented as full GA.

## 4. Full GA Procedure

Full GA starts only after public release closure and requires all external evidence.

### F-1: Complete Live OIDC Evidence

Use `docs/commercial/oidc-live-evidence-template.md` to create `docs/commercial/oidc-live-evidence.md`.

Required evidence includes:

- backend and frontend commit SHAs (40-char lowercase hex, both equal to the image release commit),
- `Keycloak or OIDC provider name` and `Provider version` fields filled with real values,
- issuer and `Registered callback URL`,
- `Run finished at` timestamp (ISO 8601 UTC),
- login start,
- IdP login event,
- callback success,
- MMMail session issuance,
- authenticated API call,
- token refresh,
- logout,
- callback error path,
- `mmmail.oidc.callback` success and error traces with correlated request IDs,
- gate run URL or command log.

The file may contain `Evidence status: completed-external-evidence` only after all required fields are real and non-empty. Field labels must match `scripts/validate-v22-external-evidence.sh` exactly, otherwise `require_nonempty_field` rejects the file.

### F-2: Complete Image Digest Evidence

Use `docs/release/image-digest-evidence-template.md` to create `docs/release/image-digest-evidence.md`.

Required evidence includes:

- `Release tag` (matches the published GA tag),
- `Git commit SHA` (40-char lowercase hex, equal to the tag commit and to OIDC/billing commits),
- `GitHub workflow run URL` for the `MMMail Images` push-event run,
- `GitHub release URL`,
- backend and frontend image names (`mmmail-backend`, `mmmail-frontend-admin`),
- `Backend immutable digest` and `Frontend immutable digest` with real `sha256:` digests (no `sha256:*` wildcard placeholder),
- the literal strings `Workflow event: push` and `Workflow conclusion: success` in the file body.

The GitHub Release for the same tag must include both image names and digests byte-for-byte identical to the evidence file.

### F-3: Complete Private Billing Evidence

Use `docs/billing/private-billing-evidence-template.md` to create `docs/billing/private-billing-evidence.md`.

Required evidence includes (field labels must match the verifier):

- accessible `IMG-LTD/mmmail-billing-gateway` repository metadata, recorded as `Billing repository URL`,
- `Billing repository commit SHA`,
- `Public MMMail repository commit SHA` (40-char lowercase hex, equal to the image/OIDC commit),
- `Payment provider` and `Provider environment` (no `none` and no mock values),
- webhook endpoint and provider delivery record,
- customer portal boundary or redirect evidence,
- invoice and refund lifecycle evidence,
- `License signing key location` plus license signing evidence using a private key outside the public repository (the body must contain the literal `License signing` token),
- idempotent replay evidence,
- `Run finished at` timestamp.

Do not use `Access requires separate authorization` as completed evidence. That phrase may document a current blocker, but it cannot satisfy full GA.

### F-4: Mark Completion Only After Full Verifier Passes

Set evidence file environment variables:

```bash
export MMMAIL_OIDC_LIVE_EVIDENCE_FILE=docs/commercial/oidc-live-evidence.md
export MMMAIL_IMAGE_DIGEST_EVIDENCE_FILE=docs/release/image-digest-evidence.md
export MMMAIL_PRIVATE_BILLING_EVIDENCE_FILE=docs/billing/private-billing-evidence.md
```

Then run:

```bash
timeout 120s bash scripts/validate-v22-external-evidence.sh
```

Required result:

- exit code `0`,
- output includes `v2.2 external evidence markers are complete`,
- no incomplete markers remain.

Only after that result may these status fields be changed:

- `docs/v22-completion-audit.md`: `audit_status: complete`
- `docs/v22-external-evidence-checklist.md`: `status: complete`
- `docs/v22-open-source-commercial-spec.md`: status updated to full GA wording

## 5. Acceptance Matrix

| Gate | Public release closure | Full GA closure |
|---|---|---|
| Spec committed and CI green | Required | Required |
| `read:packages` available | Required | Required |
| Public tag exists and points to the recorded acceptance commit | Required | Required |
| `MMMail Images` tag-push run success | Required | Required |
| GitHub Release notes include image digests | Required | Required |
| OIDC evidence completed | Required only if public release claims live OIDC verification | Required |
| Private billing evidence completed | Not claimed | Required |
| Full external verifier | Run and record expected failure if private billing remains external | Must pass |
| Completion audit status | Remains `not-complete-external-evidence-required` unless full verifier passes | May become `complete` only after full verifier passes |

## 6. Failure Standards

| Condition | Result |
|---|---|
| Full verifier fails | Full GA is blocked |
| Full verifier is modified to skip private billing | Invalid GA evidence |
| Private billing evidence says completed but repo/provider/signing evidence is unavailable | Invalid evidence |
| Public release notes imply payment-ready or full GA without billing evidence | Invalid public release |
| Image digest evidence contains `sha256:*` or missing release notes | Blocked |
| Evidence files reference different Public MMMail commits | Blocked |
| GHCR package API returns HTTP 403 | Permission gap, not proof of package visibility |
| Tag points to a commit different from the commit recorded inside the evidence metadata | Blocked |
| Main CI or tag image workflow fails | Blocked |

## 7. One-Line Target

First close `v2.2.0-public` as a scoped public release without weakening the full external verifier; then close full `v2.2.0` only after real live OIDC, image digest, GHCR, release notes, and private billing evidence make `scripts/validate-v22-external-evidence.sh` pass with exit code 0.
