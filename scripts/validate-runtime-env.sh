#!/usr/bin/env bash
set -euo pipefail

ENV_FILE="${1:-.env}"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "env file not found: $ENV_FILE" >&2
  exit 1
fi

while IFS= read -r line || [[ -n "$line" ]]; do
  [[ -z "$line" || "${line#\#}" != "$line" ]] && continue
  key="${line%%=*}"
  value="${line#*=}"
  export "$key=$value"
done < "$ENV_FILE"

required_keys=(
  NUXT_PUBLIC_AUTH_CSRF_COOKIE_NAME
  MMMAIL_JWT_SECRET
  SPRING_DATASOURCE_USERNAME
  SPRING_DATASOURCE_PASSWORD
  SPRING_REDIS_PASSWORD
  NACOS_USERNAME
  NACOS_PASSWORD
  MYSQL_ROOT_PASSWORD
)

errors=()
for key in "${required_keys[@]}"; do
  value="${!key:-}"
  if [[ -z "$value" ]]; then
    errors+=("$key is missing")
    continue
  fi
  if [[ "$value" == replace-with-* ]]; then
    errors+=("$key still uses placeholder value")
  fi
done

sql_init_mode="${SPRING_SQL_INIT_MODE:-never}"
if [[ "$sql_init_mode" != "never" ]]; then
  errors+=("SPRING_SQL_INIT_MODE must be 'never' because Flyway owns schema migrations")
fi

if (( ${#errors[@]} > 0 )); then
  printf '%s\n' "${errors[@]}" >&2
  exit 1
fi

echo "runtime env validation passed: $ENV_FILE"
