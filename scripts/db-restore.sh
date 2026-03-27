#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
ENV_FILE="${1:-.env}"
BACKUP_DIR="${2:-}"

if [[ -z "$BACKUP_DIR" ]]; then
  echo "backup directory is required" >&2
  exit 1
fi

source "$ROOT_DIR/scripts/lib/db-common.sh"

load_env_file "$ENV_FILE"
require_datasource_env
resolve_drive_path
parse_mysql_jdbc_url "$SPRING_DATASOURCE_URL"
require_mysql_client_support

DATABASE_DUMP="$BACKUP_DIR/database.sql"
DRIVE_ARCHIVE="$BACKUP_DIR/drive-data.tar.gz"
MANIFEST_FILE="$BACKUP_DIR/manifest.txt"

if [[ ! -f "$DATABASE_DUMP" || ! -f "$DRIVE_ARCHIVE" || ! -f "$MANIFEST_FILE" ]]; then
  echo "backup payload is incomplete: $BACKUP_DIR" >&2
  exit 1
fi

DROP_SQL="$(mysql_cli -N -B -e "select concat('drop table if exists \`', table_name, '\`;') from information_schema.tables where table_schema = '${DB_NAME}' and table_type = 'BASE TABLE'")"
if [[ -n "$DROP_SQL" ]]; then
  printf 'SET FOREIGN_KEY_CHECKS=0;\n%s\nSET FOREIGN_KEY_CHECKS=1;\n' "$DROP_SQL" | mysql_cli
fi

mysql_cli < "$DATABASE_DUMP"

rm -rf "$MMMAIL_BACKUP_DRIVE_PATH"
mkdir -p "$MMMAIL_BACKUP_DRIVE_PATH"
tar -xzf "$DRIVE_ARCHIVE" -C "$MMMAIL_BACKUP_DRIVE_PATH"

echo "restore completed from $BACKUP_DIR"
