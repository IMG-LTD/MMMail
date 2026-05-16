#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

DEFAULT_REPORT_DIR="${TMPDIR:-/tmp}/mmmail-security"
REPORT_DIR="${MMMAIL_SECURITY_REPORT_DIR:-$DEFAULT_REPORT_DIR}"
REPORT_FILE="$REPORT_DIR/secret-scan.txt"
mkdir -p "$REPORT_DIR"

mapfile -t TRACKED_FILES < <(
  while IFS= read -r -d '' file; do
    [[ -f "$file" ]] || continue
    case "$file" in
      pnpm-lock.yaml|package-lock.json|yarn.lock|*/pnpm-lock.yaml|*/package-lock.json|*/yarn.lock)
        continue
        ;;
    esac
    printf '%s\n' "$file"
  done < <(git ls-files --cached --others --exclude-standard -z)
)

trim_value() {
  local value="$1"
  value="${value%%#*}"
  value="${value%%//*}"
  value="${value#${value%%[![:space:]]*}}"
  value="${value%${value##*[![:space:]]}}"
  value="${value#\"}"
  value="${value%\"}"
  value="${value#\'}"
  value="${value%\'}"
  value="${value%\\}"
  value="${value%${value##*[![:space:]]}}"
  value="${value%)}"
  value="${value%;}"
  value="${value%${value##*[![:space:]]}}"
  printf '%s\n' "$value"
}

is_safe_config_value() {
  local lowered
  lowered="$(printf '%s' "$1" | tr '[:upper:]' '[:lower:]')"
  [[ -z "$lowered" ]] && return 0
  [[ "$lowered" == \{* || "$lowered" == \[* ]] && return 0
  [[ "$lowered" == true || "$lowered" == false || "$lowered" == null ]] && return 0
  [[ "$lowered" == read || "$lowered" == write || "$lowered" == none ]] && return 0
  [[ "$lowered" == \$* ]] && return 0
  [[ "$lowered" == \\\$\{* ]] && return 0
  [[ "$lowered" == \$\{* ]] && return 0
  case "$lowered" in
    replace-with-*|test-*|dummy*|example*|changeme*|localhost*|127.0.0.1*|0.0.0.0*|http://*|https://*)
      return 0
      ;;
    0123456789abcdef0123456789abcdef|nacos|mmmail_refresh_token|mmmail_csrf_token|mmmail_test_password|redis_test_password)
      return 0
      ;;
  esac
  return 1
}

scan_hard_patterns() {
  local pattern
  pattern="-----BEGIN (RSA|OPENSSH|EC|DSA|PGP|PRIVATE KEY)-----|AKIA[0-9A-Z]{16}|AIza[0-9A-Za-z_-]{35}|ghp_[0-9A-Za-z]{36}|xox[baprs]-[0-9A-Za-z-]{10,48}"
  printf '%s\0' "${TRACKED_FILES[@]}" \
    | xargs -0r grep -nEH -- "$pattern" || true
}

scan_config_assignments() {
  local found=0
  while IFS= read -r file; do
    while IFS= read -r match; do
      local line_no="${match%%:*}"
      local line="${match#*:}"
      if [[ "$line" == *"replace-with-"* ]]; then
        continue
      fi
      local value="$line"
      if [[ "$value" == *"="* ]]; then
        value="${value#*=}"
      elif [[ "$value" == *":"* ]]; then
        value="${value#*:}"
      fi
      value="$(trim_value "$value")"
      if is_safe_config_value "$value"; then
        continue
      fi
      printf '%s:%s:%s\n' "$file" "$line_no" "$line"
      found=1
    done < <(
      grep -nEi \
        '(^|[^[:alnum:]_])(password|secret|token|api[_-]?key|access[_-]?key|private[_-]?key)[^:=]{0,32}[:=][^#]+' \
        "$file" || true
    )
  done < <(
    printf '%s\n' "${TRACKED_FILES[@]}" \
      | grep -E '(^config/)|(^\.env)|(^\.github/workflows/)|(^backend/.*/application.*\.(yml|yaml|properties)$)|(\.(yml|yaml|properties|json|txt|sh)$)'
  )
  return "$found"
}

{
  printf 'MMMail secret regression scan\n'
  printf 'Date: %s\n' "$(date '+%Y-%m-%d %H:%M:%S')"
  printf 'Tracked files: %s\n\n' "${#TRACKED_FILES[@]}"
} > "$REPORT_FILE"

hard_matches="$(scan_hard_patterns)"
config_matches=""
if ! config_matches="$(scan_config_assignments)"; then
  :
fi

if [[ -n "$hard_matches" || -n "$config_matches" ]]; then
  {
    printf 'Suspicious hardcoded secrets detected.\n\n'
    if [[ -n "$hard_matches" ]]; then
      printf '[hard-patterns]\n%s\n\n' "$hard_matches"
    fi
    if [[ -n "$config_matches" ]]; then
      printf '[config-assignments]\n%s\n' "$config_matches"
    fi
  } >> "$REPORT_FILE"
  cat "$REPORT_FILE" >&2
  exit 1
fi

printf 'No suspicious hardcoded secrets detected.\n' >> "$REPORT_FILE"
cat "$REPORT_FILE"
