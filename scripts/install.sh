#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${MMMAIL_ENV_FILE:-$ROOT_DIR/.env}"
REQUESTED_MODE="${1:-}"

print_usage() {
  printf '%s\n' "Usage: scripts/install.sh [minimal|standard]"
  printf '%s\n' "Set MMMAIL_ENV_FILE=/path/to/.env to use a custom env file."
}

has_command() {
  command -v "$1" >/dev/null 2>&1
}

require_command() {
  if ! has_command "$1"; then
    printf '%s\n' "Missing required command: $1" >&2
    exit 1
  fi
}

read_env_value() {
  local key="$1"
  local line
  line="$(grep -E "^${key}=" "$ENV_FILE" | tail -n 1 || true)"
  printf '%s' "${line#*=}"
}

require_env_value() {
  local key="$1"
  local value
  value="$(read_env_value "$key")"

  if [[ -z "$value" ]]; then
    printf '%s\n' "$key is missing in $ENV_FILE" >&2
    exit 1
  fi

  if [[ "$value" == replace-with-* ]]; then
    printf '%s\n' "$key still uses placeholder value in $ENV_FILE" >&2
    exit 1
  fi
}

require_env_equals() {
  local key="$1"
  local expected="$2"
  local value
  value="$(read_env_value "$key")"

  if [[ "$value" != "$expected" ]]; then
    printf '%s\n' "$key must be $expected for this install mode" >&2
    exit 1
  fi
}

ensure_env_file() {
  if [[ -f "$ENV_FILE" ]]; then
    return
  fi

  cp "$ROOT_DIR/.env.example" "$ENV_FILE"
  printf '%s\n' "Created $ENV_FILE from .env.example."
  printf '%s\n' "Edit $ENV_FILE and replace required secrets before running this installer again."
  exit 1
}

select_mode() {
  case "$REQUESTED_MODE" in
    minimal|standard)
      printf '%s' "$REQUESTED_MODE"
      return
      ;;
    -h|--help)
      print_usage
      exit 0
      ;;
    '')
      ;;
    *)
      print_usage >&2
      exit 1
      ;;
  esac

  if [[ -t 0 ]]; then
    printf '%s' "Choose install mode [minimal/standard] (minimal): " >&2
    read -r selected_mode
    selected_mode="${selected_mode:-minimal}"
  else
    selected_mode="minimal"
  fi

  case "$selected_mode" in
    minimal|standard)
      printf '%s' "$selected_mode"
      ;;
    *)
      printf '%s\n' "Unknown install mode: $selected_mode" >&2
      exit 1
      ;;
  esac
}

check_env_for_mode() {
  local mode="$1"

  require_env_value MMMAIL_JWT_SECRET
  require_env_value SPRING_DATASOURCE_PASSWORD
  require_env_value SPRING_REDIS_PASSWORD
  require_env_value MYSQL_ROOT_PASSWORD

  if [[ "$mode" == "minimal" ]]; then
    # Requires MMMAIL_NACOS_ENABLED=false
    require_env_equals MMMAIL_NACOS_ENABLED false
  else
    # Requires MMMAIL_NACOS_ENABLED=true
    require_env_equals MMMAIL_NACOS_ENABLED true
    require_env_value NACOS_USERNAME
    require_env_value NACOS_PASSWORD
  fi
}

run_compose() {
  local mode="$1"

  cd "$ROOT_DIR"

  if [[ "$mode" == "minimal" ]]; then
    docker compose --env-file "$ENV_FILE" -f docker-compose.minimal.yml up -d --build
  else
    docker compose --env-file "$ENV_FILE" up -d --build
  fi
}

print_success() {
  local mode="$1"

  printf '\n%s\n' "MMMail $mode mode is starting."
  printf '%s\n' "Frontend: http://127.0.0.1:3001"
  printf '%s\n' "Backend health: http://127.0.0.1:8080/actuator/health"
  printf '%s\n' "Boundary page: http://127.0.0.1:3001/boundary"
  printf '%s\n' "Migration status: ./scripts/db-upgrade.sh .env info"
}

main() {
  require_command docker
  docker compose version >/dev/null
  ensure_env_file

  local mode
  mode="$(select_mode)"
  check_env_for_mode "$mode"
  bash "$ROOT_DIR/scripts/validate-runtime-env.sh" "$ENV_FILE"
  run_compose "$mode"
  print_success "$mode"
}

main "$@"
