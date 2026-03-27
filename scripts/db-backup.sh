#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
ENV_FILE="${1:-.env}"
OUTPUT_DIR="${2:-$ROOT_DIR/backups/$(date +%Y%m%d-%H%M%S)}"

source "$ROOT_DIR/scripts/lib/db-common.sh"

load_env_file "$ENV_FILE"
require_datasource_env
resolve_drive_path
parse_mysql_jdbc_url "$SPRING_DATASOURCE_URL"
require_mysql_client_support

mkdir -p "$OUTPUT_DIR" "$MMMAIL_BACKUP_DRIVE_PATH"

SCHEMA_VERSION="$(mysql_cli -N -B -e "select coalesce((select version from flyway_schema_history where success = 1 and version is not null order by installed_rank desc limit 1), 'none')")"

mysqldump_cli --single-transaction --skip-comments --no-tablespaces > "$OUTPUT_DIR/database.sql"
tar -C "$MMMAIL_BACKUP_DRIVE_PATH" -czf "$OUTPUT_DIR/drive-data.tar.gz" .

{
  echo "timestamp=$(date -Iseconds)"
  echo "database=$DB_NAME"
  echo "jdbc_url=$SPRING_DATASOURCE_URL"
  echo "drive_path=$MMMAIL_BACKUP_DRIVE_PATH"
  echo "schema_version=$SCHEMA_VERSION"
} > "$OUTPUT_DIR/manifest.txt"

echo "$OUTPUT_DIR"
