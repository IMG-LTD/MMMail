#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

source "$ROOT_DIR/scripts/lib/java-common.sh"

MVN_BIN="$(resolve_maven_bin "$ROOT_DIR")"
BACKEND_SECURITY_TESTS="DependencyVersionGuardTest,VapidWebPushDeliveryGatewayConfigurationTest,SecurityBaselineIntegrationTest,DriveSecureShareIntegrationTest,DrivePublicFolderShareIntegrationTest,MailAttachmentIntegrationTest"

echo "[validate-security] secret regression scan"
bash scripts/security-secret-scan.sh >/tmp/mmmail-security-secret-scan.log 2>&1

echo "[validate-security] backend security dependencies warmup"
env \
  SPRING_DATASOURCE_PASSWORD=test-password \
  MMMAIL_JWT_SECRET=0123456789abcdef0123456789abcdef \
  NACOS_USERNAME=test-nacos-user \
  NACOS_PASSWORD=test-nacos-password \
  "$MVN_BIN" -f backend/pom.xml -pl mmmail-server -am -DskipTests test-compile \
  >/tmp/mmmail-backend-security-warmup.log 2>&1

echo "[validate-security] backend security regression"
timeout 60s env \
  SPRING_DATASOURCE_PASSWORD=test-password \
  MMMAIL_JWT_SECRET=0123456789abcdef0123456789abcdef \
  NACOS_USERNAME=test-nacos-user \
  NACOS_PASSWORD=test-nacos-password \
  "$MVN_BIN" -f backend/pom.xml -pl mmmail-server -am \
  -Dtest="$BACKEND_SECURITY_TESTS" -Dsurefire.failIfNoSpecifiedTests=false test \
  >/tmp/mmmail-backend-security.log 2>&1

if [[ "${MMMAIL_RUN_BACKEND_DEPENDENCY_SCAN:-false}" == "true" ]]; then
  echo "[validate-security] backend dependency scan"
  bash scripts/security-backend-dependency-scan.sh >/tmp/mmmail-dependency-scan.log 2>&1
  report_dir="${MMMAIL_SECURITY_REPORT_DIR:-$ROOT_DIR/artifacts/security}/dependency-check"
  required_reports=(
    "$report_dir/dependency-check-report.html"
    "$report_dir/dependency-check-report.json"
  )
  for report in "${required_reports[@]}"; do
    if [[ ! -f "$report" ]]; then
      echo "missing dependency scan report: $report" >&2
      exit 1
    fi
  done
else
  echo "[validate-security] skip backend dependency scan (set MMMAIL_RUN_BACKEND_DEPENDENCY_SCAN=true to enable locally)"
fi

echo "[validate-security] security gates passed"
