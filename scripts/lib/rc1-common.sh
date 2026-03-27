#!/usr/bin/env bash
set -euo pipefail

prepare_rc1_env() {
  local output_file="$1"
  local drive_root="$2"
  local backup_drive_path="$3"

  cp .env.example "$output_file"
  sed -i 's/replace-with-32-plus-char-random-secret/0123456789abcdef0123456789abcdef/' "$output_file"
  sed -i 's/replace-with-db-password/DbPassword123!/' "$output_file"
  sed -i 's/replace-with-mysql-root-password/MySqlRoot123!/' "$output_file"
  sed -i 's/replace-with-redis-password/RedisPassword123!/' "$output_file"
  sed -i 's/replace-with-nacos-user/nacos/' "$output_file"
  sed -i 's/replace-with-nacos-password/nacos/' "$output_file"
  sed -i "s|^MMMAIL_DRIVE_STORAGE_ROOT=.*$|MMMAIL_DRIVE_STORAGE_ROOT=$drive_root|" "$output_file"
  if [[ -n "$backup_drive_path" ]]; then
    printf 'MMMAIL_BACKUP_DRIVE_PATH=%s\n' "$backup_drive_path" >> "$output_file"
  fi
}

wait_for_http_ok() {
  local url="$1"
  local label="$2"
  local timeout_seconds="$3"
  local elapsed=0

  while (( elapsed < timeout_seconds )); do
    if curl -fsS "$url" >/dev/null 2>&1; then
      return 0
    fi
    sleep 5
    elapsed=$((elapsed + 5))
  done

  echo "$label did not become healthy within ${timeout_seconds}s: $url" >&2
  return 1
}

sync_drive_from_container() {
  local container_name="$1"
  local target_dir="$2"

  rm -rf "$target_dir"
  mkdir -p "$target_dir"
  docker cp "${container_name}:/var/lib/mmmail/drive/." "$target_dir"
}

sync_drive_to_container() {
  local source_dir="$1"
  local container_name="$2"

  docker exec "$container_name" sh -lc 'mkdir -p /var/lib/mmmail/drive && rm -rf /var/lib/mmmail/drive/*'
  docker cp "${source_dir}/." "${container_name}:/var/lib/mmmail/drive"
}

write_rc1_report_header() {
  local report_file="$1"
  local title="$2"

  cat > "$report_file" <<EOF
# ${title}

- Generated at: $(date -Iseconds)
- Host: $(hostname)

## Evidence
EOF
}
