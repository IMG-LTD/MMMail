#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
BACKEND_DIR="$ROOT_DIR/backend"
SERVER_PORT="${SERVER_PORT:-8080}"
SPRING_PROFILE="${SPRING_PROFILE:-local}"

cd "$BACKEND_DIR"
echo "[backend] install latest mmmail-common snapshot"
mvn -pl mmmail-common -am -DskipTests install

echo "[backend] start mmmail-server"
echo "[backend] profile=$SPRING_PROFILE port=$SERVER_PORT"
cd "$BACKEND_DIR/mmmail-server"
exec mvn -DskipTests spring-boot:run \
  -Dspring-boot.run.profiles="$SPRING_PROFILE" \
  -Dspring-boot.run.arguments="--server.port=$SERVER_PORT"
