#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

audit="docs/v22-completion-audit.md"
checklist="docs/v22-external-evidence-checklist.md"
spec="docs/v22-open-source-commercial-spec.md"
security="SECURITY.md"
oidc_template="docs/commercial/oidc-live-evidence-template.md"
image_template="docs/release/image-digest-evidence-template.md"
billing_template="docs/billing/private-billing-evidence-template.md"

missing=()

require_file() {
  local file="$1"
  if [[ ! -f "$file" ]]; then
    echo "missing required evidence file: $file" >&2
    exit 1
  fi
}

contains() {
  local file="$1"
  local pattern="$2"
  grep -Fq "$pattern" "$file"
}

require_contains() {
  local file="$1"
  local pattern="$2"
  local description="$3"
  if ! contains "$file" "$pattern"; then
    echo "missing external evidence checklist coverage: $description" >&2
    exit 1
  fi
}

record_if_present() {
  local file="$1"
  local pattern="$2"
  local message="$3"
  if contains "$file" "$pattern"; then
    missing+=("$message")
  fi
}

record_if_env_file_missing() {
  local env_name="$1"
  local message="$2"
  local path="${!env_name:-}"
  if [[ -z "$path" || ! -f "$path" ]]; then
    missing+=("$message")
  fi
}

record_if_command_fails() {
  local message="$1"
  shift
  if ! "$@" >/dev/null 2>&1; then
    missing+=("$message")
  fi
}

# Keep marker strings literal for root governance contracts:
# - backend GHCR package versions are not visible
# - frontend-admin GHCR package versions are not visible
record_if_ghcr_versions_unavailable() {
  local package_name="$1"
  local label="$2"
  local message="$label GHCR package versions are not visible"
  local output
  if output="$(gh api "orgs/IMG-LTD/packages/container/$package_name/versions" 2>&1)"; then
    return 0
  fi
  printf '%s\n' "$output" >/tmp/mmmail-v22-external-evidence-check.log
  if [[ "$output" == *"read:packages"* || "$output" == *"HTTP 403"* ]]; then
    missing+=("$message (GitHub API requires gh auth with read:packages scope)")
    return 0
  fi
  missing+=("$message")
}

record_if_v22_release_notes_unavailable() {
  local output
  if output="$(gh release list --repo IMG-LTD/MMMail --limit 20 2>/dev/null)" \
    && printf '%s\n' "$output" | grep -Eq '(^|[[:space:]])v2\.2'; then
    return 0
  fi
  missing+=("v2.2 image digest release notes are not visible")
}

record_if_unpublished_worktree() {
  local tracked_count untracked_count ahead_count
  tracked_count="$(git diff --name-only | wc -l | tr -d '[:space:]')"
  untracked_count="$(git ls-files --others --exclude-standard | wc -l | tr -d '[:space:]')"
  ahead_count="$(git rev-list --count origin/main..HEAD 2>/dev/null || printf '0')"
  ahead_count="$(printf '%s' "$ahead_count" | tr -d '[:space:]')"
  if [[ "$tracked_count" != "0" || "$untracked_count" != "0" || "$ahead_count" != "0" ]]; then
    missing+=("current v2.2 implementation is not published to a remote commit/tag")
  fi
}

require_command() {
  local command_name="$1"
  if ! command -v "$command_name" >/dev/null 2>&1; then
    echo "missing required external evidence command: $command_name" >&2
    exit 1
  fi
}

require_env_file() {
  local env_name="$1"
  local description="$2"
  local path="${!env_name:-}"
  if [[ -z "$path" || ! -f "$path" ]]; then
    echo "missing required external evidence file for $description: $env_name" >&2
    exit 1
  fi
}

reject_contains() {
  local file="$1"
  local pattern="$2"
  local description="$3"
  if contains "$file" "$pattern"; then
    echo "invalid external evidence content: $description" >&2
    exit 1
  fi
}

require_completed_evidence_file() {
  local file="$1"
  local description="$2"
  require_contains "$file" "Evidence status: completed-external-evidence" "$description completion marker"
  reject_contains "$file" "This template defines" "$description uses a template as evidence"
  reject_contains "$file" "Create one redacted evidence package" "$description uses an unfilled template"
}

require_nonempty_field() {
  local file="$1"
  local field="$2"
  local description="$3"
  if ! grep -Eq "^-?[[:space:]]*${field}: [^[:space:]].*" "$file"; then
    echo "missing completed external evidence field: $description" >&2
    exit 1
  fi
}

field_value() {
  local file="$1"
  local field="$2"
  local line
  line="$(grep -E "^-?[[:space:]]*${field}: [^[:space:]].*" "$file" | head -n 1 || true)"
  if [[ -z "$line" ]]; then
    printf ''
    return 0
  fi
  printf '%s\n' "${line#*: }"
}

require_hex_sha() {
  local value="$1"
  local description="$2"
  if [[ ! "$value" =~ ^[0-9a-f]{40}$ ]]; then
    echo "invalid completed external evidence commit SHA: $description" >&2
    exit 1
  fi
}

require_equal_value() {
  local actual="$1"
  local expected="$2"
  local description="$3"
  if [[ "$actual" != "$expected" ]]; then
    echo "mismatched completed external evidence value: $description" >&2
    exit 1
  fi
}

remote_tag_commit() {
  local tag="$1"
  local output direct peeled
  if [[ ! "$tag" =~ ^[A-Za-z0-9._/-]+$ ]]; then
    echo "invalid completed external evidence release tag: $tag" >&2
    exit 1
  fi
  if ! output="$(git ls-remote --tags origin "refs/tags/$tag" "refs/tags/$tag^{}" 2>&1)"; then
    echo "external evidence command failed: release tag lookup" >&2
    printf '%s\n' "$output" >&2
    exit 1
  fi
  direct="$(printf '%s\n' "$output" | awk -v ref="refs/tags/$tag" '$2 == ref { print $1; exit }')"
  peeled="$(printf '%s\n' "$output" | awk -v ref="refs/tags/$tag^{}" '$2 == ref { print $1; exit }')"
  if [[ -n "$peeled" ]]; then
    printf '%s\n' "$peeled"
    return 0
  fi
  if [[ -n "$direct" ]]; then
    printf '%s\n' "$direct"
    return 0
  fi
  echo "release tag is not visible on origin: $tag" >&2
  exit 1
}

verify_publication_preconditions() {
  local oidc_backend_commit oidc_frontend_commit image_commit billing_public_commit release_tag tag_commit branches
  oidc_backend_commit="$(field_value "$MMMAIL_OIDC_LIVE_EVIDENCE_FILE" "Backend commit SHA")"
  oidc_frontend_commit="$(field_value "$MMMAIL_OIDC_LIVE_EVIDENCE_FILE" "Frontend commit SHA")"
  image_commit="$(field_value "$MMMAIL_IMAGE_DIGEST_EVIDENCE_FILE" "Git commit SHA")"
  billing_public_commit="$(field_value "$MMMAIL_PRIVATE_BILLING_EVIDENCE_FILE" "Public MMMail repository commit SHA")"
  release_tag="$(field_value "$MMMAIL_IMAGE_DIGEST_EVIDENCE_FILE" "Release tag")"
  require_hex_sha "$oidc_backend_commit" "OIDC backend commit SHA"
  require_hex_sha "$oidc_frontend_commit" "OIDC frontend commit SHA"
  require_hex_sha "$image_commit" "image commit SHA"
  require_hex_sha "$billing_public_commit" "billing public MMMail commit SHA"
  require_equal_value "$oidc_frontend_commit" "$image_commit" "OIDC frontend commit must match image commit"
  require_equal_value "$oidc_backend_commit" "$image_commit" "OIDC backend commit must match image commit"
  require_equal_value "$billing_public_commit" "$image_commit" "billing public MMMail commit must match image commit"
  tag_commit="$(remote_tag_commit "$release_tag")"
  require_equal_value "$tag_commit" "$image_commit" "release tag must point to the image commit"
  if ! branches="$(git branch -r --contains "$image_commit" 2>&1)"; then
    echo "external evidence command failed: release commit remote branch containment" >&2
    printf '%s\n' "$branches" >&2
    exit 1
  fi
  if ! printf '%s\n' "$branches" | grep -Eq '^[[:space:]]*origin/(main|release/.+)$'; then
    echo "release commit is not visible on origin/main or origin/release/*: $image_commit" >&2
    exit 1
  fi
}

require_command_output() {
  local description="$1"
  shift
  local output
  if ! output="$("$@" 2>&1)"; then
    echo "external evidence command failed: $description" >&2
    printf '%s\n' "$output" >&2
    exit 1
  fi
  if [[ -z "$output" || "$output" == "[]" ]]; then
    echo "external evidence command returned no evidence: $description" >&2
    exit 1
  fi
  printf '%s\n' "$output" >/tmp/mmmail-v22-external-evidence-check.log
}

verify_image_release_notes() {
  local release_tag backend_digest frontend_digest body
  release_tag="$(field_value "$MMMAIL_IMAGE_DIGEST_EVIDENCE_FILE" "Release tag")"
  backend_digest="$(field_value "$MMMAIL_IMAGE_DIGEST_EVIDENCE_FILE" "Backend immutable digest")"
  frontend_digest="$(field_value "$MMMAIL_IMAGE_DIGEST_EVIDENCE_FILE" "Frontend immutable digest")"
  if ! body="$(gh release view "$release_tag" --repo IMG-LTD/MMMail --json body --jq .body 2>&1)"; then
    echo "external evidence command failed: image digest release notes" >&2
    printf '%s\n' "$body" >&2
    exit 1
  fi
  for expected in "mmmail-backend" "mmmail-frontend-admin" "$backend_digest" "$frontend_digest"; do
    if ! printf '%s\n' "$body" | grep -Fq "$expected"; then
      echo "missing completed external evidence in release notes: $expected" >&2
      exit 1
    fi
  done
}

verify_private_vulnerability_reporting() {
  require_command gh
  local output
  if ! output="$(gh api --method GET repos/IMG-LTD/MMMail/private-vulnerability-reporting 2>&1)"; then
    echo "external evidence command failed: GitHub private vulnerability reporting" >&2
    printf '%s\n' "$output" >&2
    exit 1
  fi
  if [[ "$output" != *'"enabled":true'* ]]; then
    echo "GitHub private vulnerability reporting is not enabled" >&2
    exit 1
  fi
}

verify_completed_external_evidence() {
  require_command gh
  require_env_file MMMAIL_OIDC_LIVE_EVIDENCE_FILE "live Keycloak/OIDC evidence"
  require_env_file MMMAIL_IMAGE_DIGEST_EVIDENCE_FILE "tag image digest evidence"
  require_env_file MMMAIL_PRIVATE_BILLING_EVIDENCE_FILE "private billing evidence"
  require_completed_evidence_file "$MMMAIL_OIDC_LIVE_EVIDENCE_FILE" "live Keycloak/OIDC evidence"
  require_completed_evidence_file "$MMMAIL_IMAGE_DIGEST_EVIDENCE_FILE" "tag image digest evidence"
  require_completed_evidence_file "$MMMAIL_PRIVATE_BILLING_EVIDENCE_FILE" "private billing evidence"
  require_nonempty_field "$MMMAIL_OIDC_LIVE_EVIDENCE_FILE" "Backend commit SHA" "OIDC backend commit SHA"
  require_nonempty_field "$MMMAIL_OIDC_LIVE_EVIDENCE_FILE" "Frontend commit SHA" "OIDC frontend commit SHA"
  require_nonempty_field "$MMMAIL_OIDC_LIVE_EVIDENCE_FILE" "Keycloak or OIDC provider name" "OIDC provider name"
  require_nonempty_field "$MMMAIL_OIDC_LIVE_EVIDENCE_FILE" "Provider version" "OIDC provider version"
  require_nonempty_field "$MMMAIL_OIDC_LIVE_EVIDENCE_FILE" "Registered callback URL" "OIDC callback URL"
  require_nonempty_field "$MMMAIL_OIDC_LIVE_EVIDENCE_FILE" "Run finished at" "OIDC run finished timestamp"
  require_nonempty_field "$MMMAIL_IMAGE_DIGEST_EVIDENCE_FILE" "Release tag" "image release tag"
  require_nonempty_field "$MMMAIL_IMAGE_DIGEST_EVIDENCE_FILE" "Git commit SHA" "image commit SHA"
  require_nonempty_field "$MMMAIL_IMAGE_DIGEST_EVIDENCE_FILE" "GitHub workflow run URL" "image workflow run URL"
  require_nonempty_field "$MMMAIL_IMAGE_DIGEST_EVIDENCE_FILE" "GitHub release URL" "image digest release notes URL"
  require_nonempty_field "$MMMAIL_IMAGE_DIGEST_EVIDENCE_FILE" "Backend immutable digest" "backend immutable digest"
  require_nonempty_field "$MMMAIL_IMAGE_DIGEST_EVIDENCE_FILE" "Frontend immutable digest" "frontend immutable digest"
  require_nonempty_field "$MMMAIL_PRIVATE_BILLING_EVIDENCE_FILE" "Billing repository URL" "billing repository URL"
  require_nonempty_field "$MMMAIL_PRIVATE_BILLING_EVIDENCE_FILE" "Billing repository commit SHA" "billing repository commit SHA"
  require_nonempty_field "$MMMAIL_PRIVATE_BILLING_EVIDENCE_FILE" "Public MMMail repository commit SHA" "billing public MMMail repository commit SHA"
  require_nonempty_field "$MMMAIL_PRIVATE_BILLING_EVIDENCE_FILE" "Payment provider" "billing payment provider"
  require_nonempty_field "$MMMAIL_PRIVATE_BILLING_EVIDENCE_FILE" "Provider environment" "billing provider environment"
  require_nonempty_field "$MMMAIL_PRIVATE_BILLING_EVIDENCE_FILE" "License signing key location" "license signing key location"
  require_nonempty_field "$MMMAIL_PRIVATE_BILLING_EVIDENCE_FILE" "Run finished at" "billing run finished timestamp"
  verify_publication_preconditions
  require_contains "$MMMAIL_OIDC_LIVE_EVIDENCE_FILE" "mmmail.oidc.callback" "live OIDC trace evidence"
  require_contains "$MMMAIL_IMAGE_DIGEST_EVIDENCE_FILE" "sha256:" "immutable image digest evidence"
  require_contains "$MMMAIL_IMAGE_DIGEST_EVIDENCE_FILE" "Workflow event: push" "tag-triggered image workflow evidence"
  require_contains "$MMMAIL_IMAGE_DIGEST_EVIDENCE_FILE" "Workflow conclusion: success" "successful image workflow evidence"
  reject_contains "$MMMAIL_IMAGE_DIGEST_EVIDENCE_FILE" "sha256:*" "image digest evidence still has wildcard digest placeholder"
  verify_image_release_notes
  require_contains "$MMMAIL_PRIVATE_BILLING_EVIDENCE_FILE" "License signing" "private billing license signing evidence"
  require_command_output "successful tag-triggered MMMail Images workflow run exists" gh run list --repo IMG-LTD/MMMail --workflow "MMMail Images" --event push --status success --limit 1
  require_command_output "backend GHCR package exists" gh api orgs/IMG-LTD/packages/container/mmmail-backend/versions
  require_command_output "frontend-admin GHCR package exists" gh api orgs/IMG-LTD/packages/container/mmmail-frontend-admin/versions
  require_command_output "private billing repository is accessible" gh repo view IMG-LTD/mmmail-billing-gateway --json name,owner,visibility,isPrivate,defaultBranchRef,url
}

require_file "$audit"
require_file "$checklist"
require_file "$spec"
require_file "$security"
require_file "$oidc_template"
require_file "$image_template"
require_file "$billing_template"

require_contains \
  "$checklist" \
  '| GitHub private vulnerability reporting | `gh api --method GET repos/IMG-LTD/MMMail/private-vulnerability-reporting` returned `{"enabled":true}` on 2026-05-17 |' \
  "GitHub private vulnerability reporting completed evidence row"

require_contains \
  "$security" \
  "GitHub private vulnerability reporting is enabled for this repository." \
  "GitHub private vulnerability reporting enabled security policy"

verify_private_vulnerability_reporting

require_contains \
  "$checklist" \
  "| BUS-01 live Keycloak SSO | Real Keycloak or approved OIDC IdP |" \
  "BUS-01 live Keycloak SSO required evidence row"

require_contains \
  "$oidc_template" \
  "not a substitute for a real Keycloak or approved OIDC IdP run" \
  "OIDC live evidence template real IdP boundary"

require_contains \
  "$oidc_template" \
  "mmmail.oidc.callback" \
  "OIDC live evidence template trace span"

require_contains \
  "$checklist" \
  "| OBS-01 live OIDC trace evidence | Same live Keycloak/OIDC run as BUS-01 |" \
  "OBS-01 live OIDC trace evidence required evidence row"

require_contains \
  "$checklist" \
  "| GATE-01 live Keycloak e2e gate | Real release or CI gate run against Keycloak/OIDC |" \
  "GATE-01 live Keycloak e2e gate required evidence row"

require_contains \
  "$checklist" \
  "| DEP-02 image digest | Real tag push workflow |" \
  "DEP-02 image digest required evidence row"

require_contains \
  "$image_template" \
  "not a substitute for a real tag-triggered image publishing workflow run" \
  "image digest evidence template real workflow boundary"

require_contains \
  "$image_template" \
  "Backend immutable digest" \
  "image digest evidence template backend digest"

require_contains \
  "$checklist" \
  "| Private billing repository | Independent billing repository and payment provider sandbox/live environment |" \
  "private billing repository required evidence row"

require_contains \
  "$billing_template" \
  "not a substitute for a real private billing repository" \
  "private billing evidence template real repository boundary"

require_contains \
  "$billing_template" \
  "License signing" \
  "private billing evidence template license signing"

record_if_present \
  "$audit" \
  "audit_status: not-complete-external-evidence-required" \
  "completion audit is still marked not-complete-external-evidence-required"

record_if_present \
  "$checklist" \
  "status: pending-external-evidence" \
  "external evidence checklist is still pending"

record_if_present \
  "$spec" \
  "| BUS-01 | partial done |" \
  "BUS-01 live Keycloak SSO remains partial"

record_if_present \
  "$spec" \
  "| DEP-02 | partial done |" \
  "DEP-02 image digest evidence remains partial"

record_if_present \
  "$spec" \
  "| OBS-01 | partial done |" \
  "OBS-01 live OIDC trace evidence remains partial"

record_if_present \
  "$spec" \
  "| GATE-01 | partial done |" \
  "GATE-01 live Keycloak e2e gate remains partial"

record_if_present \
  "$audit" \
  "External private billing repository required" \
  "private billing repository and real payment evidence remain external"

record_if_env_file_missing \
  "MMMAIL_OIDC_LIVE_EVIDENCE_FILE" \
  "live OIDC evidence file is not provided"

record_if_env_file_missing \
  "MMMAIL_IMAGE_DIGEST_EVIDENCE_FILE" \
  "image digest evidence file is not provided"

record_if_env_file_missing \
  "MMMAIL_PRIVATE_BILLING_EVIDENCE_FILE" \
  "private billing evidence file is not provided"

record_if_unpublished_worktree

record_if_command_fails \
  "successful tag-triggered MMMail Images workflow run is not visible" \
  gh run list --repo IMG-LTD/MMMail --workflow "MMMail Images" --event push --status success --limit 1

record_if_v22_release_notes_unavailable

record_if_ghcr_versions_unavailable "mmmail-backend" "backend"

record_if_ghcr_versions_unavailable "mmmail-frontend-admin" "frontend-admin"

record_if_command_fails \
  "private billing repository is not accessible" \
  gh repo view IMG-LTD/mmmail-billing-gateway --json name,owner,visibility,isPrivate,defaultBranchRef,url

if ((${#missing[@]} > 0)); then
  echo "v2.2 external evidence is incomplete:" >&2
  for item in "${missing[@]}"; do
    echo "- $item" >&2
  done
  exit 1
fi

verify_completed_external_evidence
echo "v2.2 external evidence markers are complete"
