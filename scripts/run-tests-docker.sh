#!/usr/bin/env bash
# scripts/run-tests-docker.sh
# v2.1.3 收尾期统一测试入口。测试数据层必须使用 docker 隔离；E2E 使用宿主机真实后端与前端，
# 避免基础镜像源异常时绕过真实运行路径。
#
# 用法：
#   newgrp docker                            # 每个新终端首次执行
#   ./scripts/run-tests-docker.sh            # 默认全套：backend + frontend-contract + unit + e2e
#   ./scripts/run-tests-docker.sh backend    # 仅后端集成
#   ./scripts/run-tests-docker.sh contract   # 仅前端 v212 合约
#   ./scripts/run-tests-docker.sh unit       # 仅前端 unit + component
#   ./scripts/run-tests-docker.sh e2e        # 仅前端 e2e（Docker mysql/redis + 宿主机 backend/frontend）
#   ./scripts/run-tests-docker.sh e2e --setup-only    # 仅启动 e2e 栈（给 Playwright globalSetup）
#   ./scripts/run-tests-docker.sh e2e --teardown-only # 仅清理 e2e 栈（给 Playwright globalTeardown）
#   ./scripts/run-tests-docker.sh --keep ... # 失败或成功后保留容器供排查

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

# shellcheck source=./check-docker-group.sh
source "$ROOT_DIR/scripts/check-docker-group.sh"

COMPOSE_FILE="${MMMAIL_TEST_COMPOSE_FILE:-docker-compose.minimal.yml}"
ENV_FILE="${MMMAIL_TEST_ENV_FILE:-.env.test}"
KEEP_CONTAINERS=0
SETUP_ONLY=0
TEARDOWN_ONLY=0
TARGETS=()
RUNTIME_DIR="${MMMAIL_TEST_RUNTIME_DIR:-${TMPDIR:-/tmp}/mmmail-test-runtime}"
BACKEND_PID_FILE="${MMMAIL_E2E_BACKEND_PID_FILE:-$RUNTIME_DIR/e2e-backend.pid}"
BACKEND_LOG_FILE="${MMMAIL_E2E_BACKEND_LOG_FILE:-$RUNTIME_DIR/e2e-backend.log}"
BACKEND_PORT="${MMMAIL_E2E_BACKEND_PORT:-18080}"
BACKEND_BASE_URL="http://127.0.0.1:${BACKEND_PORT}"
BACKEND_HEALTH_URL="${MMMAIL_E2E_BACKEND_HEALTH_URL:-$BACKEND_BASE_URL/actuator/health}"
BACKEND_WAIT_ATTEMPTS=90
BACKEND_WAIT_INTERVAL_SECONDS=2

while [[ $# -gt 0 ]]; do
  case "$1" in
    --keep) KEEP_CONTAINERS=1 ;;
    --setup-only) SETUP_ONLY=1 ;;
    --teardown-only) TEARDOWN_ONLY=1 ;;
    backend|contract|unit|e2e|all) TARGETS+=("$1") ;;
    -h|--help)
      grep -E '^# ' "$0" | sed 's/^# //'
      exit 0
      ;;
    *) echo "[run-tests-docker] 未知参数: $1" >&2; exit 1 ;;
  esac
  shift
done

if [[ ${#TARGETS[@]} -eq 0 ]]; then
  TARGETS=(backend contract unit e2e)
fi

require_docker_access

ensure_env_file() {
  if [[ ! -f "$ENV_FILE" ]]; then
    echo "[run-tests-docker] 缺 $ENV_FILE，从 .env.example 派生测试环境..."
    cp .env.example "$ENV_FILE"
    # 替换占位密码，避免容器拒启
    sed -i \
      -e 's|replace-with-32-plus-char-random-secret|test-jwt-secret-32-bytes-minimum-aaaa|g' \
      -e 's|replace-with-db-password|mmmail_test_password|g' \
      -e 's|replace-with-mysql-root-password|root_test_password|g' \
      -e 's|replace-with-redis-password|redis_test_password|g' \
      -e 's|SPRING_PROFILES_ACTIVE=local|SPRING_PROFILES_ACTIVE=local,dev|g' \
      "$ENV_FILE"
  fi
}

ensure_env_file

compose() {
  docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" "$@"
}

cleanup() {
  local rc=$?
  if [[ $KEEP_CONTAINERS -eq 1 ]]; then
    echo "[run-tests-docker] --keep 已设，保留容器（rc=$rc）"
    echo "  排查后清理: docker compose -f $COMPOSE_FILE down -v"
    return
  fi
  stop_host_backend
  echo "[run-tests-docker] 清理容器与卷..."
  compose down -v --remove-orphans >/dev/null 2>&1 || true
}

start_infra() {
  echo "[run-tests-docker] 清理残留容器与卷（如有）..."
  compose down -v --remove-orphans >/dev/null 2>&1 || true
  echo "[run-tests-docker] 启动 mysql + redis..."
  compose up -d --wait mysql redis
}

start_full_stack() {
  stop_host_backend
  start_infra
  start_host_backend
}

load_env_file() {
  set -a
  # shellcheck disable=SC1090
  source "$ENV_FILE"
  set +a
}

start_host_backend() {
  if curl -fsS "$BACKEND_HEALTH_URL" >/dev/null 2>&1; then
    echo "[run-tests-docker] backend 已在 127.0.0.1:8080 运行，请先停止现有进程后重试。" >&2
    exit 1
  fi

  load_env_file
  export SERVER_PORT="$BACKEND_PORT"
  mkdir -p "$(dirname "$BACKEND_PID_FILE")"
  rm -f "$BACKEND_PID_FILE" "$BACKEND_LOG_FILE"
  echo "[run-tests-docker] 构建 backend 依赖..."
  mvn -q -f backend/pom.xml -pl mmmail-server -am -DskipTests install
  echo "[run-tests-docker] 启动宿主机 backend（$BACKEND_BASE_URL，日志: $BACKEND_LOG_FILE）..."
  setsid bash -c '
    set -euo pipefail
    cd "$1"
    exec mvn -q -f backend/mmmail-server/pom.xml \
      -DskipTests \
      -Dspring-boot.run.main-class=com.mmmail.server.MmmailServerApplication \
      spring-boot:run
  ' _ "$ROOT_DIR" </dev/null >"$BACKEND_LOG_FILE" 2>&1 &
  echo "$!" > "$BACKEND_PID_FILE"
  wait_for_backend
}

wait_for_backend() {
  local pid
  pid="$(cat "$BACKEND_PID_FILE")"
  for _ in $(seq 1 "$BACKEND_WAIT_ATTEMPTS"); do
    if curl -fsS "$BACKEND_HEALTH_URL" >/dev/null 2>&1; then
      echo "[run-tests-docker] backend health ready"
      return 0
    fi
    if ! kill -0 "$pid" >/dev/null 2>&1; then
      echo "[run-tests-docker] backend 启动失败，日志尾部:" >&2
      tail -80 "$BACKEND_LOG_FILE" >&2 || true
      exit 1
    fi
    sleep "$BACKEND_WAIT_INTERVAL_SECONDS"
  done
  echo "[run-tests-docker] backend health 超时，日志尾部:" >&2
  tail -80 "$BACKEND_LOG_FILE" >&2 || true
  exit 1
}

stop_host_backend() {
  if [[ ! -f "$BACKEND_PID_FILE" ]]; then
    return
  fi
  local pid
  pid="$(cat "$BACKEND_PID_FILE")"
  if kill -0 "$pid" >/dev/null 2>&1; then
    echo "[run-tests-docker] 停止宿主机 backend..."
    kill "$pid" >/dev/null 2>&1 || true
    wait "$pid" >/dev/null 2>&1 || true
  fi
  rm -f "$BACKEND_PID_FILE"
}

run_backend() {
  start_infra
  echo "[run-tests-docker] 后端集成测试..."
  ( cd "$ROOT_DIR" && mvn -f backend/pom.xml -pl mmmail-server -am test \
      -Dspring.datasource.url="jdbc:mysql://127.0.0.1:3306/mmmail?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true" \
	      -Dspring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver \
	      -Dspring.datasource.username=mmmail_app \
	      -Dspring.datasource.password=mmmail_test_password \
	      -Dspring.flyway.enabled=true \
	      -Dspring.sql.init.mode=never \
	      -Dspring.redis.host=127.0.0.1 \
	      -Dspring.redis.password=redis_test_password )
}

run_contract() {
  start_full_stack
  echo "[run-tests-docker] 前端 v212 合约..."
  ( cd frontend-admin && pnpm test:v212 )
}

run_unit() {
  echo "[run-tests-docker] 前端 unit + component（无需容器）..."
  ( cd frontend-admin && pnpm test:coverage )
}

run_e2e() {
  start_full_stack
  echo "[run-tests-docker] 前端 e2e..."
  (
    cd frontend-admin
    MMMAIL_E2E_SKIP_DOCKER_SETUP=1 \
      MMMAIL_E2E_API_BASE_URL="$BACKEND_BASE_URL" \
      VITE_SERVICE_BASE_URL="$BACKEND_BASE_URL" \
      pnpm test:e2e
  )
}

if [[ $TEARDOWN_ONLY -eq 1 ]]; then
  echo "[run-tests-docker] 清理 e2e 栈..."
  stop_host_backend
  compose down -v --remove-orphans
  exit 0
fi

if [[ $SETUP_ONLY -eq 1 ]]; then
  trap cleanup EXIT
  start_full_stack
  trap - EXIT
  echo "[run-tests-docker] e2e 栈已启动"
  exit 0
fi

trap cleanup EXIT

for target in "${TARGETS[@]}"; do
  case "$target" in
    backend)  run_backend ;;
    contract) run_contract ;;
    unit)     run_unit ;;
    e2e)      run_e2e ;;
  esac
done

echo "[run-tests-docker] 全部测试通过"
