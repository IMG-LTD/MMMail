#!/usr/bin/env bash
set -euo pipefail

CONTAINER_MYSQL_IMAGE="${CONTAINER_MYSQL_IMAGE:-mysql:8.4}"

load_env_file() {
  local env_file="$1"
  while IFS= read -r line || [[ -n "$line" ]]; do
    [[ -z "$line" || "${line#\#}" != "$line" ]] && continue
    local key="${line%%=*}"
    local value="${line#*=}"
    export "$key=$value"
  done < "$env_file"
}

require_datasource_env() {
  local keys=(
    SPRING_DATASOURCE_URL
    SPRING_DATASOURCE_USERNAME
    SPRING_DATASOURCE_PASSWORD
  )
  local missing=()
  for key in "${keys[@]}"; do
    local value="${!key:-}"
    if [[ -z "$value" ]]; then
      missing+=("$key")
    fi
  done
  if (( ${#missing[@]} > 0 )); then
    printf 'missing required db env: %s\n' "${missing[*]}" >&2
    exit 1
  fi
}

resolve_drive_path() {
  local resolved="${MMMAIL_BACKUP_DRIVE_PATH:-${MMMAIL_DRIVE_STORAGE_ROOT:-}}"
  if [[ -z "$resolved" ]]; then
    echo "missing required drive path env: MMMAIL_DRIVE_STORAGE_ROOT" >&2
    exit 1
  fi
  export MMMAIL_BACKUP_DRIVE_PATH="$resolved"
}

parse_mysql_jdbc_url() {
  local jdbc_url="$1"
  if [[ ! "$jdbc_url" =~ ^jdbc:mysql://([^:/?#]+):([0-9]+)/([^?]+) ]]; then
    echo "unsupported MySQL JDBC url: $jdbc_url" >&2
    exit 1
  fi
  export DB_HOST="${BASH_REMATCH[1]}"
  export DB_PORT="${BASH_REMATCH[2]}"
  export DB_NAME="${BASH_REMATCH[3]}"
}

docker_client_available() {
  command -v docker >/dev/null 2>&1 && docker info >/dev/null 2>&1
}

require_mysql_client_support() {
  if command -v mysql >/dev/null 2>&1 && command -v mysqldump >/dev/null 2>&1; then
    return 0
  fi
  if docker_client_available; then
    return 0
  fi
  echo "mysql/mysqldump not found and docker daemon is unavailable; cannot execute backup or restore" >&2
  exit 1
}

mysql_cli() {
  if command -v mysql >/dev/null 2>&1; then
    MYSQL_PWD="$SPRING_DATASOURCE_PASSWORD" \
      mysql -h "$DB_HOST" -P "$DB_PORT" --protocol tcp -u "$SPRING_DATASOURCE_USERNAME" "$DB_NAME" "$@"
    return
  fi
  if docker_client_available; then
    docker run --rm --network host \
      -e MYSQL_PWD="$SPRING_DATASOURCE_PASSWORD" \
      "$CONTAINER_MYSQL_IMAGE" \
      mysql -h "$DB_HOST" -P "$DB_PORT" --protocol tcp -u "$SPRING_DATASOURCE_USERNAME" "$DB_NAME" "$@"
    return
  fi
  echo "mysql client is unavailable and docker daemon is inaccessible" >&2
  exit 1
}

mysqldump_cli() {
  if command -v mysqldump >/dev/null 2>&1; then
    MYSQL_PWD="$SPRING_DATASOURCE_PASSWORD" \
      mysqldump -h "$DB_HOST" -P "$DB_PORT" --protocol tcp -u "$SPRING_DATASOURCE_USERNAME" "$DB_NAME" "$@"
    return
  fi
  if docker_client_available; then
    docker run --rm --network host \
      -e MYSQL_PWD="$SPRING_DATASOURCE_PASSWORD" \
      "$CONTAINER_MYSQL_IMAGE" \
      mysqldump -h "$DB_HOST" -P "$DB_PORT" --protocol tcp -u "$SPRING_DATASOURCE_USERNAME" "$DB_NAME" "$@"
    return
  fi
  echo "mysqldump client is unavailable and docker daemon is inaccessible" >&2
  exit 1
}
