#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

source "$ROOT_DIR/scripts/lib/db-common.sh"
source "$ROOT_DIR/scripts/lib/rc1-common.sh"

if ! docker_client_available; then
  echo "RC1 container validation requires a Docker-capable runner" >&2
  exit 1
fi

ARTIFACT_DIR="${MMMAIL_RC1_CONTAINER_ARTIFACT_DIR:-$ROOT_DIR/artifacts/release/rc1-container}"
REPORT_FILE="$ARTIFACT_DIR/community-v1-rc1-container-evidence.md"
ENV_FILE="$ARTIFACT_DIR/rc1-container.env"
DRIVE_ROOT="$ARTIFACT_DIR/drive-runtime"
DRIVE_MIRROR="$ARTIFACT_DIR/drive-mirror"
BACKUP_DIR="$ARTIFACT_DIR/backups/pre-upgrade"
ROLLBACK_DIR="$ARTIFACT_DIR/backups/pre-rollback"
COMPOSE_LOG="$ARTIFACT_DIR/compose.log"
COMPOSE_PS="$ARTIFACT_DIR/compose-ps.log"
COMPOSE_CONFIG_LOG="$ARTIFACT_DIR/compose-config.log"
UPGRADE_INFO_LOG="$ARTIFACT_DIR/db-info.log"
UPGRADE_VALIDATE_LOG="$ARTIFACT_DIR/db-validate.log"
UPGRADE_LOG="$ARTIFACT_DIR/db-upgrade.log"
BACKUP_LOG="$ARTIFACT_DIR/db-backup.log"
RESTORE_LOG="$ARTIFACT_DIR/db-restore.log"
ROLLBACK_LOG="$ARTIFACT_DIR/db-rollback.log"

rel_path() {
  local path="$1"
  printf '%s' "${path#$ROOT_DIR/}"
}

mkdir -p "$ARTIFACT_DIR" "$DRIVE_ROOT" "$DRIVE_MIRROR" "$BACKUP_DIR" "$ROLLBACK_DIR"
prepare_rc1_env "$ENV_FILE" "$DRIVE_ROOT" "$DRIVE_MIRROR"
load_env_file "$ENV_FILE"
require_datasource_env
resolve_drive_path
parse_mysql_jdbc_url "$SPRING_DATASOURCE_URL"
require_mysql_client_support

compose() {
  docker compose --env-file "$ENV_FILE" -f "$ROOT_DIR/docker-compose.yml" "$@"
}

cleanup() {
  compose ps >"$COMPOSE_PS" 2>&1 || true
  compose logs --no-color >"$COMPOSE_LOG" 2>&1 || true
  compose down -v >/dev/null 2>&1 || true
}
trap cleanup EXIT

docker compose --env-file "$ENV_FILE" -f "$ROOT_DIR/docker-compose.yml" config >"$COMPOSE_CONFIG_LOG" 2>&1
compose down -v >/dev/null 2>&1 || true
compose up -d --build >/dev/null

wait_for_http_ok "http://127.0.0.1:8080/actuator/health" "backend health" 240
wait_for_http_ok "http://127.0.0.1:3001" "frontend health" 240

"$ROOT_DIR/scripts/db-upgrade.sh" "$ENV_FILE" info >"$UPGRADE_INFO_LOG" 2>&1
"$ROOT_DIR/scripts/db-upgrade.sh" "$ENV_FILE" validate >"$UPGRADE_VALIDATE_LOG" 2>&1
"$ROOT_DIR/scripts/db-upgrade.sh" "$ENV_FILE" upgrade >"$UPGRADE_LOG" 2>&1

seed_users="$(mysql_cli -N -B -e "select count(*) from user_account")"
schema_version="$(mysql_cli -N -B -e "select schema_version from system_release_metadata order by id desc limit 1")"
latest_flyway_version="$(mysql_cli -N -B -e "select coalesce((select version from flyway_schema_history where success = 1 and version is not null order by installed_rank desc limit 1), 'none')")"
if (( seed_users < 2 )); then
  echo "seed data validation failed: expected at least 2 users, got $seed_users" >&2
  exit 1
fi
if [[ "$schema_version" != "$latest_flyway_version" ]]; then
  echo "unexpected schema version after upgrade: metadata=$schema_version flyway=$latest_flyway_version" >&2
  exit 1
fi

mysql_cli <<'SQL'
insert into user_account (id, email, password_hash, display_name, role, status, token_version, created_at, updated_at, deleted)
values (990001, 'rc1-backup@mmmail.local', 'hash', 'RC1 Backup', 'USER', 1, 1, current_timestamp, current_timestamp, 0);
SQL
docker exec mmmail-backend sh -lc "mkdir -p /var/lib/mmmail/drive && printf 'backup-original' > /var/lib/mmmail/drive/rc1-marker.txt"
sync_drive_from_container "mmmail-backend" "$DRIVE_MIRROR"
"$ROOT_DIR/scripts/db-backup.sh" "$ENV_FILE" "$BACKUP_DIR" >"$BACKUP_LOG" 2>&1

mysql_cli -e "delete from user_account where id = 990001"
docker exec mmmail-backend sh -lc "printf 'backup-mutated' > /var/lib/mmmail/drive/rc1-marker.txt"
"$ROOT_DIR/scripts/db-restore.sh" "$ENV_FILE" "$BACKUP_DIR" >"$RESTORE_LOG" 2>&1
sync_drive_to_container "$DRIVE_MIRROR" "mmmail-backend"

restored_count="$(mysql_cli -N -B -e "select count(*) from user_account where id = 990001")"
restored_marker="$(docker exec mmmail-backend sh -lc "cat /var/lib/mmmail/drive/rc1-marker.txt")"
if [[ "$restored_count" != "1" || "$restored_marker" != "backup-original" ]]; then
  echo "restore validation failed" >&2
  exit 1
fi

mysql_cli <<'SQL'
insert into user_account (id, email, password_hash, display_name, role, status, token_version, created_at, updated_at, deleted)
values (990002, 'rc1-rollback@mmmail.local', 'hash', 'RC1 Rollback', 'USER', 1, 1, current_timestamp, current_timestamp, 0);
SQL
docker exec mmmail-backend sh -lc "printf 'rollback-original' > /var/lib/mmmail/drive/rc1-rollback.txt"
sync_drive_from_container "mmmail-backend" "$DRIVE_MIRROR"
"$ROOT_DIR/scripts/db-backup.sh" "$ENV_FILE" "$ROLLBACK_DIR" >"$ARTIFACT_DIR/db-backup-rollback.log" 2>&1

mysql_cli -e "delete from user_account where id = 990002"
docker exec mmmail-backend sh -lc "printf 'rollback-mutated' > /var/lib/mmmail/drive/rc1-rollback.txt"
"$ROOT_DIR/scripts/db-rollback.sh" "$ENV_FILE" "$ROLLBACK_DIR" >"$ROLLBACK_LOG" 2>&1
sync_drive_to_container "$DRIVE_MIRROR" "mmmail-backend"

rollback_count="$(mysql_cli -N -B -e "select count(*) from user_account where id = 990002")"
rollback_marker="$(docker exec mmmail-backend sh -lc "cat /var/lib/mmmail/drive/rc1-rollback.txt")"
if [[ "$rollback_count" != "1" || "$rollback_marker" != "rollback-original" ]]; then
  echo "rollback validation failed" >&2
  exit 1
fi

write_rc1_report_header "$REPORT_FILE" "Community v1.0 RC1 Container Evidence"
cat >> "$REPORT_FILE" <<EOF
- fresh install: PASS
- init/seed verification: PASS (users=${seed_users}, schema=${schema_version})
- upgrade info: PASS (\`$(rel_path "$UPGRADE_INFO_LOG")\`)
- upgrade validate: PASS (\`$(rel_path "$UPGRADE_VALIDATE_LOG")\`)
- upgrade apply: PASS (\`$(rel_path "$UPGRADE_LOG")\`)
- backup: PASS (\`$(rel_path "$BACKUP_LOG")\`)
- restore: PASS (\`$(rel_path "$RESTORE_LOG")\`)
- rollback strategy: PASS (\`$(rel_path "$ROLLBACK_LOG")\`)
- compose config: PASS (\`$(rel_path "$COMPOSE_CONFIG_LOG")\`)
- compose logs: \`$(rel_path "$COMPOSE_LOG")\`
- compose ps: \`$(rel_path "$COMPOSE_PS")\`
- backup payloads:
  - \`$(rel_path "$BACKUP_DIR")\`
  - \`$(rel_path "$ROLLBACK_DIR")\`
EOF

printf '%s\n' "$REPORT_FILE"
