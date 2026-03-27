#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
ENV_FILE="${1:-.env}"
COMMAND="${2:-upgrade}"

source "$ROOT_DIR/scripts/lib/db-common.sh"
source "$ROOT_DIR/scripts/lib/java-common.sh"

load_env_file "$ENV_FILE"
require_datasource_env

MVN_BIN="$(resolve_maven_bin "$ROOT_DIR")"
BACKEND_POM="$ROOT_DIR/backend/pom.xml"
SERVER_POM="$ROOT_DIR/backend/mmmail-server/pom.xml"
SERVER_ARTIFACTS_PREPARED=0

prepare_server_artifacts() {
  if [[ "$SERVER_ARTIFACTS_PREPARED" -eq 1 ]]; then
    return
  fi
  timeout 60s "$MVN_BIN" \
    -f "$BACKEND_POM" \
    -pl mmmail-server \
    -am \
    -DskipTests \
    install
  SERVER_ARTIFACTS_PREPARED=1
}

run_cli() {
  local action="$1"
  prepare_server_artifacts
  timeout 60s "$MVN_BIN" \
    -f "$SERVER_POM" \
    -DskipTests \
    exec:java \
    -Dexec.mainClass=com.mmmail.server.migration.MigrationCli \
    -Dexec.args="$action"
}

case "$COMMAND" in
  upgrade)
    echo "[db-upgrade] current migration state"
    run_cli info
    echo "[db-upgrade] validating"
    run_cli validate
    echo "[db-upgrade] applying migrations"
    run_cli migrate
    echo "[db-upgrade] resulting migration state"
    run_cli info
    ;;
  info|validate|migrate|repair)
    run_cli "$COMMAND"
    ;;
  *)
    echo "unsupported db-upgrade command: $COMMAND" >&2
    echo "supported commands: upgrade, info, validate, migrate, repair" >&2
    exit 1
    ;;
esac
