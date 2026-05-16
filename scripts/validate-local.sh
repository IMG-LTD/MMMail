#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

source "$ROOT_DIR/scripts/lib/db-common.sh"
source "$ROOT_DIR/scripts/lib/java-common.sh"

MVN_BIN="$(resolve_maven_bin "$ROOT_DIR")"
BACKEND_AUTH_RBAC_TESTS="AuthFlowIntegrationTest,OrgAuthenticationSecurityIntegrationTest,OrgAdminConsoleIntegrationTest,OrgMemberGovernanceIntegrationTest"
BACKEND_DOCS_TESTS="DocsCollaborationIntegrationTest,DocsSuggestionWorkflowIntegrationTest,DocsOrgAccessIntegrationTest"
BACKEND_MAIL_GA_TESTS="MailGaIntegrationTest,MailAttachmentIntegrationTest,MailReleaseBlockingIntegrationTest"
BACKEND_CALENDAR_GA_TESTS="CalendarSharingAvailabilityIntegrationTest,CalendarReleaseBlockingIntegrationTest,CalendarIcsImportIntegrationTest"
BACKEND_DRIVE_GA_TESTS="DriveReleaseBlockingIntegrationTest,DriveCollaboratorShareIntegrationTest,DriveSharedWithMeIntegrationTest,DriveSecureShareIntegrationTest,DrivePublicFolderShareIntegrationTest"
BACKEND_OBSERVABILITY_TESTS="JobRunMonitorServiceTest,GlobalExceptionHandlerUnitTest,RequestRouteModuleResolverTest,RequestObservationServiceTest,BackendV22OpenTelemetryContractTest"
BACKEND_SHEETS_TESTS="SheetsWorkbookIntegrationTest,SheetsWorkbookDataManagementIntegrationTest,SheetsSharingVersionIntegrationTest,SheetsWorkbookMultiSheetIntegrationTest"
BACKEND_V2_CONTRACT_TESTS="PlatformCapabilityIntegrationTest,PublicShareCapabilityIntegrationTest,WorkspaceAggregationIntegrationTest,AiMcpCapabilityIntegrationTest,RequestHeaderContractIntegrationTest,ObservabilityIntegrationTest,PrometheusEndpointRuntimeTest,BillingReadinessIntegrationTest,ContractCatalogRegressionTest,TenantScopeFoundationContractTest,BackendModuleExtractionContractTest,PublicShareTokenHashMigrationIntegrationTest,MailPublicShareTokenHashContractTest,PassPublicShareTokenHashContractTest,DrivePublicShareTokenHashContractTest"
BACKEND_V21_RUNTIME_TESTS="BackendV21AccessEntitlementGatesTest,BackendV21ApiContractCatalogTest,BackendV21BackgroundJobFoundationTest,BackendV21CalendarRuntimeBridgeTest,BackendV21CollaborationWriteRuntimeTest,BackendV21CommunityRuntimeClosureTest,BackendV21DocsSheetsRuntimeBridgeTest,BackendV21DriveRuntimeBridgeTest,BackendV21EventOutboxFoundationTest,BackendV21MailRuntimeBridgeTest,BackendV21OpsRuntimeBridgeTest,BackendV21PassRuntimeBridgeTest,BackendV21RuntimeContractGapClosureTest"
BACKEND_V212_COVERAGE_TESTS="WebSocketMetricsServiceTest,WebSocketChannelLimitServiceTest,WebSocketConnectionLimitServiceTest,WebSocketThrottleServiceTest"
BACKEND_V22_COMMERCIAL_TESTS="BackendV22EditionCoreContractTest,BackendV22LicenseVerifierContractTest,BackendV22BillingWebhookContractTest,BackendV22EntitlementEnforcementContractTest,BackendV22CommercialSurfaceCoverageContractTest,BackendV22LicenseManagementApiContractTest,BackendV22AuditExportContractTest,BackendV22DsrContractTest,BackendV22OidcSsoContractTest"

echo "[validate-local] root repository contract gates"
node --test tests/*.test.mjs >/tmp/mmmail-root-repository-contract.log 2>&1

echo "[validate-local] legacy frontend-v2 freeze"
bash scripts/validate-legacy-frontend-v2-freeze.sh >/tmp/mmmail-legacy-frontend-v2-freeze.log 2>&1

echo "[validate-local] mmmail admin v2.1.2 contract regression"
env -u http_proxy -u https_proxy -u HTTP_PROXY -u HTTPS_PROXY -u ALL_PROXY -u all_proxy \
  pnpm --dir frontend-admin test:v212 >/tmp/mmmail-admin-v212.log 2>&1

echo "[validate-local] mmmail admin v2.1.2 coverage gates"
env -u http_proxy -u https_proxy -u HTTP_PROXY -u HTTPS_PROXY -u ALL_PROXY -u all_proxy \
  pnpm --dir frontend-admin test:coverage >/tmp/mmmail-admin-coverage.log 2>&1

echo "[validate-local] mmmail admin v2.1.2 browser e2e"
env -u http_proxy -u https_proxy -u HTTP_PROXY -u HTTPS_PROXY -u ALL_PROXY -u all_proxy \
  pnpm --dir frontend-admin test:e2e >/tmp/mmmail-admin-e2e.log 2>&1

echo "[validate-local] mmmail admin v2.1.2 style/i18n/bundle gates"
env -u http_proxy -u https_proxy -u HTTP_PROXY -u HTTPS_PROXY -u ALL_PROXY -u all_proxy \
  pnpm --dir frontend-admin check:style-discipline >/tmp/mmmail-admin-style-discipline.log 2>&1
env -u http_proxy -u https_proxy -u HTTP_PROXY -u HTTPS_PROXY -u ALL_PROXY -u all_proxy \
  pnpm --dir frontend-admin check:i18n >/tmp/mmmail-admin-i18n.log 2>&1
env -u http_proxy -u https_proxy -u HTTP_PROXY -u HTTPS_PROXY -u ALL_PROXY -u all_proxy \
  pnpm --dir frontend-admin check:bundle-budget >/tmp/mmmail-admin-bundle-budget.log 2>&1

echo "[validate-local] mmmail admin v2.1.2 lighthouse"
env -u http_proxy -u https_proxy -u HTTP_PROXY -u HTTPS_PROXY -u ALL_PROXY -u all_proxy \
  MMMAIL_LIGHTHOUSE_SKIP_BUILD=1 pnpm --dir frontend-admin test:lighthouse >/tmp/mmmail-admin-lighthouse.log 2>&1

echo "[validate-local] security gates"
bash scripts/validate-security.sh >/tmp/mmmail-security.log 2>&1

echo "[validate-local] supply chain SBOM/license report"
node scripts/generate-sbom-license-report.mjs >/tmp/mmmail-supply-chain.log 2>&1

echo "[validate-local] helm chart"
bash scripts/validate-helm-chart.sh >/tmp/mmmail-helm-chart.log 2>&1

echo "[validate-local] image publishing contract"
node --test tests/v22-image-publishing-contract.test.mjs >/tmp/mmmail-image-publishing-contract.log 2>&1

echo "[validate-local] commercial boundaries contract"
node --test tests/v22-commercial-boundaries-contract.test.mjs >/tmp/mmmail-commercial-boundaries-contract.log 2>&1

echo "[validate-local] DSR data inventory"
node scripts/validate-dsr-inventory.mjs >/tmp/mmmail-dsr-inventory.log 2>&1

echo "[validate-local] required files"
required=(
  README.md
  AGENTS.md
  .editorconfig
  .gitattributes
  LICENSE
  DCO.md
  NOTICE
  CONTRIBUTING.md
  SUPPORT.md
  SECURITY.md
  CODE_OF_CONDUCT.md
  GOVERNANCE.md
  ROADMAP.md
  MAINTAINERS.md
  .github/CODEOWNERS
  .github/dependabot.yml
  .github/pull_request_template.md
  .github/workflows/images.yml
  .github/workflows/dco.yml
  .github/ISSUE_TEMPLATE/security-contact-request.md
  .github/ISSUE_TEMPLATE/config.yml
  .github/ISSUE_TEMPLATE/bug-report.md
  .github/ISSUE_TEMPLATE/release-blocking-regression.md
  .github/ISSUE_TEMPLATE/self-hosting-feedback.md
  .github/ISSUE_TEMPLATE/feature-request.md
  docs/ops/install.md
  docs/ops/helm.md
  docs/ops/upgrade.md
  docs/ops/backup-restore.md
  docs/ops/runbook.md
  docs/release/v2-support-boundaries.md
  docs/release/v2-feedback-intake.md
  docs/release/v2.1.2-shipping-clean-release-notes.md
  docs/release/image-digest-evidence-template.md
  docs/v22-completion-audit.md
  docs/v22-external-evidence-checklist.md
  docs/billing/private-billing-evidence-template.md
  docs/frontend/v22-frontend-topology-audit.md
  docs/frontend/v22-frontend-convergence-decision.md
  docs/commercial/pricing-boundaries.md
  docs/commercial/support-policy.md
  docs/commercial/trademark-policy.md
  docs/commercial/edition-entitlement-surface.md
  docs/commercial/oidc-sso.md
  docs/commercial/oidc-live-evidence-template.md
  docs/compliance/audit-export.md
  docs/compliance/dsr.md
  docs/compliance/data-inventory.yaml
  docs/observability/sli-slo.md
  docs/observability/opentelemetry.md
  docs/open-source/module-maturity-matrix.md
  docs/open-source/README.en.md
  docs/ops/install.en.md
  docs/architecture/deployment-topology.md
  docs/architecture/database-migration-strategy.md
  docs/security/threat-model.md
  docker-compose.yml
  docker-compose.minimal.yml
  backend/Dockerfile
  backend/pom.xml
  frontend-admin/Dockerfile
  helm/mmmail/Chart.yaml
  helm/mmmail/values.yaml
  helm/mmmail/templates/backend-configmap.yaml
  helm/mmmail/templates/backend-deployment.yaml
  helm/mmmail/templates/backend-secret.yaml
  helm/mmmail/templates/frontend-deployment.yaml
  helm/mmmail/templates/ingress.yaml
  .env.example
  config/backend.env.example
  config/backend.test.env.example
  scripts/security-secret-scan.sh
  scripts/generate-sbom-license-report.mjs
  scripts/validate-dsr-inventory.mjs
  scripts/validate-v22-external-evidence.sh
  scripts/validate-helm-chart.sh
  scripts/security-backend-dependency-scan.sh
  scripts/validate-security.sh
  scripts/validate-runtime-env.sh
  scripts/validate-backend-test-env.sh
  scripts/validate-batch3.sh
  scripts/validate-local.sh
  scripts/validate-legacy-frontend-v2-freeze.sh
  scripts/validate-ci.sh
  scripts/db-upgrade.sh
  scripts/db-backup.sh
  scripts/db-restore.sh
  scripts/db-rollback.sh
)
for file in "${required[@]}"; do
  if [[ ! -f "$file" ]]; then
    echo "missing required file: $file" >&2
    exit 1
  fi
done

echo "[validate-local] sanitized secrets in config templates"
placeholder_checks=(
  ".env.example|MMMAIL_NACOS_ENABLED=false"
  ".env.example|VITE_API_BASE_URL=http://localhost:8080"
  ".env.example|MMMAIL_JWT_SECRET=replace-with-32-plus-char-random-secret"
  ".env.example|MMMAIL_JWT_SECRET_FILE="
  ".env.example|MMMAIL_BILLING_WEBHOOK_SECRET=replace-with-32-plus-char-random-secret"
  ".env.example|MMMAIL_LICENSE_PUBLIC_KEY="
  ".env.example|MMMAIL_OIDC_ENABLED=false"
  ".env.example|MMMAIL_OIDC_CALLBACK_PATH=/api/v2/auth/oidc/callback"
  ".env.example|MMMAIL_OIDC_CLIENT_SECRET="
  ".env.example|MMMAIL_OTEL_ENABLED=false"
  ".env.example|MMMAIL_OTEL_SAMPLING_PROBABILITY=1.0"
  ".env.example|OTEL_SERVICE_NAME=mmmail-server"
  ".env.example|OTEL_TRACES_EXPORTER=none"
  ".env.example|OTEL_EXPORTER_OTLP_ENDPOINT="
  ".env.example|NACOS_AUTH_TOKEN=replace-with-32-plus-char-random-secret"
  "config/backend.env.example|MMMAIL_NACOS_ENABLED=true"
  "config/backend.env.example|SPRING_DATASOURCE_PASSWORD=replace-with-db-password"
  "config/backend.env.example|SPRING_REDIS_PASSWORD=replace-with-redis-password"
  "config/backend.env.example|NACOS_PASSWORD=replace-with-nacos-password"
  "config/backend.env.example|NACOS_AUTH_TOKEN=replace-with-32-plus-char-random-secret"
  "config/backend.env.example|MMMAIL_JWT_SECRET=replace-with-32-plus-char-random-secret"
  "config/backend.env.example|MMMAIL_JWT_SECRET_FILE="
  "config/backend.env.example|MMMAIL_BILLING_WEBHOOK_SECRET=replace-with-32-plus-char-random-secret"
  "config/backend.env.example|MMMAIL_LICENSE_PUBLIC_KEY="
  "config/backend.env.example|MMMAIL_OIDC_ENABLED=false"
  "config/backend.env.example|MMMAIL_OIDC_CALLBACK_PATH=/api/v2/auth/oidc/callback"
  "config/backend.env.example|MMMAIL_OIDC_CLIENT_SECRET="
  "config/backend.env.example|MMMAIL_OTEL_ENABLED=false"
  "config/backend.env.example|MMMAIL_OTEL_SAMPLING_PROBABILITY=1.0"
  "config/backend.env.example|OTEL_SERVICE_NAME=mmmail-server"
  "config/backend.env.example|OTEL_TRACES_EXPORTER=none"
  "config/backend.env.example|OTEL_EXPORTER_OTLP_ENDPOINT="
  "backend/mmmail-server/src/main/resources/application-local.yml|      enabled: \${MMMAIL_NACOS_ENABLED:true}"
  "backend/mmmail-server/src/main/resources/application-local.yml|    password: \${SPRING_DATASOURCE_PASSWORD:replace-with-db-password}"
  "backend/mmmail-server/src/main/resources/application-local.yml|      password: \${SPRING_REDIS_PASSWORD:replace-with-redis-password}"
  "backend/mmmail-server/src/main/resources/application-local.yml|      username: \${NACOS_USERNAME:replace-with-nacos-user}"
  "backend/mmmail-server/src/main/resources/application-local.yml|      password: \${NACOS_PASSWORD:replace-with-nacos-password}"
  "backend/mmmail-server/src/main/resources/application-local.yml|  jwt-secret: \${MMMAIL_JWT_SECRET:replace-with-32-plus-char-random-secret}"
  "backend/mmmail-server/src/main/resources/application-local.yml|  jwt-secret-file: \${MMMAIL_JWT_SECRET_FILE:}"
)
for entry in "${placeholder_checks[@]}"; do
  file="${entry%%|*}"
  expected_line="${entry#*|}"
  if ! grep -Fqx -- "$expected_line" "$file"; then
    echo "missing sanitized placeholder in $file: $expected_line" >&2
    exit 1
  fi
done

echo "[validate-local] env templates include required keys"
required_env_keys=(
  VITE_API_BASE_URL
  MMMAIL_NACOS_ENABLED
  MMMAIL_JWT_SECRET
  MMMAIL_JWT_SECRET_FILE
  MMMAIL_BILLING_WEBHOOK_SECRET
  MMMAIL_LICENSE_PUBLIC_KEY
  MMMAIL_OIDC_ENABLED
  MMMAIL_OIDC_CALLBACK_PATH
  MMMAIL_OIDC_CLIENT_SECRET
  MMMAIL_OTEL_ENABLED
  MMMAIL_OTEL_SAMPLING_PROBABILITY
  OTEL_SERVICE_NAME
  OTEL_TRACES_EXPORTER
  OTEL_EXPORTER_OTLP_ENDPOINT
  MMMAIL_CORS_ALLOWED_ORIGINS
  SPRING_DATASOURCE_URL
  SPRING_DATASOURCE_USERNAME
  SPRING_DATASOURCE_PASSWORD
  MYSQL_ROOT_PASSWORD
  NACOS_USERNAME
  NACOS_PASSWORD
  NACOS_AUTH_TOKEN
  NACOS_AUTH_IDENTITY_KEY
  NACOS_AUTH_IDENTITY_VALUE
)
for key in "${required_env_keys[@]}"; do
  if ! grep -Eq "^${key}=" .env.example config/backend.env.example; then
    echo "missing required env key in templates: $key" >&2
    exit 1
  fi
done

echo "[validate-local] runtime env template"
tmp_env_standard="$(mktemp)"
tmp_env_minimal="$(mktemp)"
trap 'rm -f "$tmp_env_standard" "$tmp_env_minimal"' EXIT
cp .env.example "$tmp_env_standard"
sed -i 's/replace-with-32-plus-char-random-secret/0123456789abcdef0123456789abcdef/' "$tmp_env_standard"
sed -i 's/replace-with-db-password/DbPassword123!/' "$tmp_env_standard"
sed -i 's/replace-with-mysql-root-password/MySqlRoot123!/' "$tmp_env_standard"
sed -i 's/replace-with-redis-password/RedisPassword123!/' "$tmp_env_standard"
sed -i 's/MMMAIL_NACOS_ENABLED=false/MMMAIL_NACOS_ENABLED=true/' "$tmp_env_standard"
sed -i 's/replace-with-nacos-user/nacos/' "$tmp_env_standard"
sed -i 's/replace-with-nacos-password/nacos/' "$tmp_env_standard"
sed -i 's/replace-with-nacos-identity-key/mmmail-nacos-identity-key/' "$tmp_env_standard"
sed -i 's/replace-with-nacos-identity-value/mmmail-nacos-identity-value/' "$tmp_env_standard"
bash scripts/validate-runtime-env.sh "$tmp_env_standard" >/tmp/mmmail-runtime-env.log 2>&1

echo "[validate-local] runtime env template (minimal mode)"
cp .env.example "$tmp_env_minimal"
sed -i 's/replace-with-32-plus-char-random-secret/0123456789abcdef0123456789abcdef/' "$tmp_env_minimal"
sed -i 's/replace-with-db-password/DbPassword123!/' "$tmp_env_minimal"
sed -i 's/replace-with-mysql-root-password/MySqlRoot123!/' "$tmp_env_minimal"
sed -i 's/replace-with-redis-password/RedisPassword123!/' "$tmp_env_minimal"
sed -i 's/MMMAIL_NACOS_ENABLED=true/MMMAIL_NACOS_ENABLED=false/' "$tmp_env_minimal"
bash scripts/validate-runtime-env.sh "$tmp_env_minimal" >/tmp/mmmail-runtime-env-minimal.log 2>&1

echo "[validate-local] compose assets"
if command -v docker >/dev/null 2>&1 && docker_client_available; then
  docker compose --env-file "$tmp_env_standard" -f docker-compose.yml config >/tmp/mmmail-docker-compose.log 2>&1
  docker compose --env-file "$tmp_env_minimal" -f docker-compose.minimal.yml config >/tmp/mmmail-docker-compose-minimal.log 2>&1
else
  echo "[validate-local] skip docker compose config: docker daemon unavailable"
fi

echo "[validate-local] secure defaults"
if grep -nE "change-me|please-change-me-before-production-use" \
  backend/mmmail-server/src/main/resources/application.yml \
  >/tmp/mmmail-insecure-defaults.log 2>&1; then
  echo "insecure default placeholder still exists in backend application.yml" >&2
  cat /tmp/mmmail-insecure-defaults.log >&2
  exit 1
fi

echo "[validate-local] db scripts syntax"
bash -n scripts/db-backup.sh scripts/db-restore.sh scripts/db-rollback.sh >/tmp/mmmail-db-scripts-syntax.log 2>&1

echo "[validate-local] backend compile"
env -u http_proxy -u https_proxy -u HTTP_PROXY -u HTTPS_PROXY -u ALL_PROXY -u all_proxy \
  "$MVN_BIN" -f backend/pom.xml -pl mmmail-server -am -DskipTests compile >/tmp/mmmail-backend-compile.log 2>&1

echo "[validate-local] backend auth/rbac regression"
timeout 60s "$MVN_BIN" -f backend/pom.xml -pl mmmail-server -am \
  -Dtest="$BACKEND_AUTH_RBAC_TESTS" -Dsurefire.failIfNoSpecifiedTests=false test \
  >/tmp/mmmail-backend-auth-rbac.log 2>&1

echo "[validate-local] backend v2 contract regression"
timeout 60s "$MVN_BIN" -f backend/pom.xml -pl mmmail-server -am \
  -Dtest="$BACKEND_V2_CONTRACT_TESTS" -Dsurefire.failIfNoSpecifiedTests=false test \
  >/tmp/mmmail-backend-v2-contract.log 2>&1

echo "[validate-local] backend v2.1 runtime regression"
timeout 60s "$MVN_BIN" -f backend/pom.xml -pl mmmail-server -am \
  -Dtest="$BACKEND_V21_RUNTIME_TESTS" -Dsurefire.failIfNoSpecifiedTests=false test \
  >/tmp/mmmail-backend-v21-runtime.log 2>&1

echo "[validate-local] backend v2.2 commercial regression"
timeout 60s "$MVN_BIN" -f backend/pom.xml -pl mmmail-server -am \
  -Dtest="$BACKEND_V22_COMMERCIAL_TESTS" -Dsurefire.failIfNoSpecifiedTests=false test \
  >/tmp/mmmail-backend-v22-commercial.log 2>&1

echo "[validate-local] backend docs regression"
timeout 60s "$MVN_BIN" -f backend/pom.xml -pl mmmail-server -am \
  -Dtest="$BACKEND_DOCS_TESTS" -Dsurefire.failIfNoSpecifiedTests=false test \
  >/tmp/mmmail-backend-docs.log 2>&1

echo "[validate-local] backend mail ga regression"
timeout 60s "$MVN_BIN" -f backend/pom.xml -pl mmmail-server -am \
  -Dtest="$BACKEND_MAIL_GA_TESTS" -Dsurefire.failIfNoSpecifiedTests=false test \
  >/tmp/mmmail-backend-mail-ga.log 2>&1

echo "[validate-local] backend calendar ga regression"
timeout 60s "$MVN_BIN" -f backend/pom.xml -pl mmmail-server -am \
  -Dtest="$BACKEND_CALENDAR_GA_TESTS" -Dsurefire.failIfNoSpecifiedTests=false test \
  >/tmp/mmmail-backend-calendar-ga.log 2>&1

echo "[validate-local] backend drive ga regression"
timeout 60s "$MVN_BIN" -f backend/pom.xml -pl mmmail-server -am \
  -Dtest="$BACKEND_DRIVE_GA_TESTS" -Dsurefire.failIfNoSpecifiedTests=false test \
  >/tmp/mmmail-backend-drive-ga.log 2>&1

echo "[validate-local] backend observability regression"
timeout 60s "$MVN_BIN" -f backend/pom.xml -pl mmmail-server -am \
  -Dtest="$BACKEND_OBSERVABILITY_TESTS" -Dsurefire.failIfNoSpecifiedTests=false test \
  >/tmp/mmmail-backend-observability.log 2>&1

echo "[validate-local] backend sheets regression"
SPRING_PROFILES_ACTIVE=test timeout 60s "$MVN_BIN" -f backend/pom.xml -pl mmmail-server -am \
  -Dtest="$BACKEND_SHEETS_TESTS" -Dsurefire.failIfNoSpecifiedTests=false test \
  >/tmp/mmmail-backend-sheets.log 2>&1

echo "[validate-local] backend coverage seed tests"
rm -f backend/mmmail-server/target/jacoco.exec
timeout 60s "$MVN_BIN" -f backend/pom.xml -pl mmmail-server -am \
  -Dtest="$BACKEND_V212_COVERAGE_TESTS" -Dsurefire.failIfNoSpecifiedTests=false test \
  >/tmp/mmmail-backend-coverage-seed.log 2>&1

echo "[validate-local] backend coverage gate"
timeout 60s "$MVN_BIN" -f backend/pom.xml -pl mmmail-server -am \
  -DskipTests verify \
  >/tmp/mmmail-backend-coverage.log 2>&1

echo "[validate-local] Batch 3 migration gates"
env -u http_proxy -u https_proxy -u HTTP_PROXY -u HTTPS_PROXY -u ALL_PROXY -u all_proxy \
  bash scripts/validate-batch3.sh >/tmp/mmmail-batch3-validate.log 2>&1

echo "[validate-local] generated type hygiene"
node frontend-admin/scripts/normalize-generated-types.mjs >/tmp/mmmail-generated-type-hygiene.log 2>&1

echo "[validate-local] diff whitespace hygiene"
git diff --check >/tmp/mmmail-diff-check.log 2>&1

echo "[validate-local] all checks passed"
