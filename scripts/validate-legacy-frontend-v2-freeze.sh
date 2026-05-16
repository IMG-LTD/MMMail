#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

BASE_REF="${MMMAIL_LEGACY_FRONTEND_BASE_REF:-origin/main}"
if ! git rev-parse --verify --quiet "$BASE_REF^{commit}" >/dev/null; then
  echo "[legacy-frontend-v2-freeze] base ref is missing: $BASE_REF" >&2
  echo "[legacy-frontend-v2-freeze] set MMMAIL_LEGACY_FRONTEND_BASE_REF to an available commit or branch" >&2
  exit 1
fi

violations_file="$(mktemp)"
trap 'rm -f "$violations_file"' EXIT

record_violation() {
  printf '%s\n' "$1" >>"$violations_file"
}

check_committed_change() {
  local status="$1" path="$2" new_path="${3:-}"
  case "$status" in
    D)
      return 0
      ;;
    R*)
      if [[ "$path" == frontend-v2/* && "$new_path" != frontend-v2/* ]]; then
        return 0
      fi
      ;;
  esac
  record_violation "$status $path${new_path:+ -> $new_path}"
}

while IFS=$'\t' read -r status path new_path; do
  [[ -z "${status:-}" ]] && continue
  check_committed_change "$status" "$path" "${new_path:-}"
done < <(git diff --name-status --find-renames "$BASE_REF"...HEAD -- frontend-v2)

while IFS= read -r line; do
  [[ -z "$line" ]] && continue
  status="${line:0:2}"
  path="${line:3}"
  case "$status" in
    " D"|"D ")
      continue
      ;;
  esac
  record_violation "$status $path"
done < <(git status --porcelain=v1 -- frontend-v2)

if [[ -s "$violations_file" ]]; then
  echo "[legacy-frontend-v2-freeze] new or modified frontend-v2 files are blocked." >&2
  echo "[legacy-frontend-v2-freeze] Only deletion or rename out of frontend-v2 is allowed during FE-03/FE-05 migration." >&2
  cat "$violations_file" >&2
  exit 1
fi

echo "[legacy-frontend-v2-freeze] frontend-v2 contains no blocked additions or modifications"
