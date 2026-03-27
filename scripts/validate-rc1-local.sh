#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

source "$ROOT_DIR/scripts/lib/db-common.sh"
source "$ROOT_DIR/scripts/lib/rc1-common.sh"

ARTIFACT_DIR="${MMMAIL_RC1_ARTIFACT_DIR:-$ROOT_DIR/artifacts/release/rc1-local}"
REPORT_FILE="$ARTIFACT_DIR/community-v1-rc1-local-evidence.md"
ENV_FILE="$ARTIFACT_DIR/rc1-local.env"
DRIVE_ROOT="$ARTIFACT_DIR/drive-root"

mkdir -p "$ARTIFACT_DIR" "$DRIVE_ROOT"
prepare_rc1_env "$ENV_FILE" "$DRIVE_ROOT" ""

RUNTIME_LOG="$ARTIFACT_DIR/runtime-env.log"
COMPOSE_CONFIG_LOG="$ARTIFACT_DIR/compose-config.log"
BATCH3_LOG="$ARTIFACT_DIR/batch3-auto.log"
LOCAL_LOG="$ARTIFACT_DIR/validate-local.log"
ALL_LOG="$ARTIFACT_DIR/validate-all.log"

rel_path() {
  local path="$1"
  printf '%s' "${path#$ROOT_DIR/}"
}

bash "$ROOT_DIR/scripts/validate-runtime-env.sh" "$ENV_FILE" >"$RUNTIME_LOG" 2>&1

compose_status="SKIPPED"
if command -v docker >/dev/null 2>&1; then
  docker compose --env-file "$ENV_FILE" -f "$ROOT_DIR/docker-compose.yml" config >"$COMPOSE_CONFIG_LOG" 2>&1
  compose_status="PASS"
else
  echo "docker CLI unavailable; compose config skipped" >"$COMPOSE_CONFIG_LOG"
fi

MMMAIL_VALIDATE_CONTAINER_TESTS=auto bash "$ROOT_DIR/scripts/validate-batch3.sh" >"$BATCH3_LOG" 2>&1
bash "$ROOT_DIR/scripts/validate-local.sh" >"$LOCAL_LOG" 2>&1
bash "$ROOT_DIR/scripts/validate-all.sh" >"$ALL_LOG" 2>&1

docker_daemon_status="NO"
if docker_client_available; then
  docker_daemon_status="YES"
fi

write_rc1_report_header "$REPORT_FILE" "Community v1.0 RC1 Local Evidence"
cat >> "$REPORT_FILE" <<EOF
- runtime env validation: PASS (\`$(rel_path "$RUNTIME_LOG")\`)
- compose config render: ${compose_status} (\`$(rel_path "$COMPOSE_CONFIG_LOG")\`)
- batch3 auto gate: PASS_OR_SKIP (\`$(rel_path "$BATCH3_LOG")\`)
- validate-local: PASS (\`$(rel_path "$LOCAL_LOG")\`)
- validate-all: PASS (\`$(rel_path "$ALL_LOG")\`)
- local evidence report: PASS (\`$(rel_path "$REPORT_FILE")\`)

## Gate 4 split
- Local completed:
  - runtime env validation
  - compose config rendering
  - default gate run
  - unified gate run
- External pending:
  - fresh install on Docker-capable runner
  - init/seed verification on live services
  - backup/restore/rollback compose workflow
  - container migration evidence
- Docker daemon available locally: ${docker_daemon_status}
EOF

printf '%s\n' "$REPORT_FILE"
