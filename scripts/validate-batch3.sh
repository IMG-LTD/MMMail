#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

source "$ROOT_DIR/scripts/lib/db-common.sh"
source "$ROOT_DIR/scripts/lib/java-common.sh"

MODE="${MMMAIL_VALIDATE_CONTAINER_TESTS:-auto}"

if ! docker_client_available; then
  if [[ "$MODE" == "true" ]]; then
    echo "docker daemon is required for Batch 3 integration validation" >&2
    exit 1
  fi
  if [[ "$MODE" == "auto" ]]; then
    echo "[validate-batch3] docker daemon unavailable, skip container-backed migration checks"
    exit 0
  fi
fi

MVN_BIN="$(resolve_maven_bin "$ROOT_DIR")"
TESTS="FlywayMigrationIntegrationTest,BackupRestoreWorkflowIntegrationTest,MigrationCliWorkflowIntegrationTest"

timeout 180s env \
  SPRING_DATASOURCE_PASSWORD=Batch3Password123! \
  MMMAIL_JWT_SECRET=0123456789abcdef0123456789abcdef \
  NACOS_USERNAME=nacos \
  NACOS_PASSWORD=nacos \
  "$MVN_BIN" -f backend/pom.xml -pl mmmail-server -am \
  -Dtest="$TESTS" -Dsurefire.failIfNoSpecifiedTests=false test
