#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

source "$ROOT_DIR/scripts/lib/db-common.sh"

ENV_FILE="${1:-${MMMAIL_BACKEND_TEST_ENV_FILE:-$ROOT_DIR/.env}}"
if [[ ! -f "$ENV_FILE" ]] && [[ -n "${1:-}" || -n "${MMMAIL_BACKEND_TEST_ENV_FILE:-}" ]]; then
  echo "backend test env file not found: $ENV_FILE" >&2
  exit 1
fi
if [[ -f "$ENV_FILE" ]]; then
  load_env_file "$ENV_FILE"
fi

normalize_boolean() {
  local raw_value="${1:-}"
  local key="$2"
  local normalized
  normalized="$(printf '%s' "$raw_value" | tr '[:upper:]' '[:lower:]')"
  case "$normalized" in
    true|false)
      printf '%s' "$normalized"
      ;;
    *)
      echo "$key must be 'true' or 'false'" >&2
      exit 1
      ;;
  esac
}

required_keys=(
  SPRING_DATASOURCE_PASSWORD
  MMMAIL_JWT_SECRET
)

nacos_enabled="$(normalize_boolean "${MMMAIL_NACOS_ENABLED:-true}" "MMMAIL_NACOS_ENABLED")"
if [[ "$nacos_enabled" == "true" ]]; then
  required_keys+=(
    NACOS_USERNAME
    NACOS_PASSWORD
  )
fi

errors=()
for key in "${required_keys[@]}"; do
  value="${!key:-}"
  if [[ -z "$value" ]]; then
    errors+=("$key is missing for backend validation")
    continue
  fi
  if [[ "$value" == replace-with-* ]]; then
    errors+=("$key still uses placeholder value")
  fi
done

if (( ${#errors[@]} > 0 )); then
  printf '%s\n' "${errors[@]}" >&2
  echo "Provide a local backend test env file or export backend validation env before running validate-local." >&2
  echo "Example: copy config/backend.test.env.example to config/backend.test.env.local and export MMMAIL_BACKEND_TEST_ENV_FILE=config/backend.test.env.local" >&2
  exit 1
fi

echo "backend test env validation passed (MMMAIL_NACOS_ENABLED=$nacos_enabled)"
