package com.mmmail.server;

import com.mmmail.server.observability.RuntimeTraceService;
import io.micrometer.common.KeyValue;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BackendV22OpenTelemetryContractTest {

    @Test
    void shouldDeclareOpenTelemetryRuntimeDependenciesAndEnvironmentConfig() throws Exception {
        String pom = read("backend/mmmail-server/pom.xml");
        String application = read("backend/mmmail-server/src/main/resources/application.yml");
        String envExample = read(".env.example");
        String backendEnvExample = read("config/backend.env.example");
        String helmConfigMap = read("helm/mmmail/templates/backend-configmap.yaml");

        assertThat(pom).contains(
                "micrometer-tracing-bridge-otel",
                "opentelemetry-exporter-otlp"
        );
        assertThat(application).contains(
                "management:",
                "tracing:",
                "enabled: ${MMMAIL_OTEL_ENABLED:false}",
                "probability: ${MMMAIL_OTEL_SAMPLING_PROBABILITY:1.0}",
                "endpoint: ${OTEL_EXPORTER_OTLP_ENDPOINT:}",
                "name: ${OTEL_SERVICE_NAME:mmmail-server}"
        );
        assertThat(envExample).contains("MMMAIL_OTEL_ENABLED=false", "OTEL_EXPORTER_OTLP_ENDPOINT=");
        assertThat(backendEnvExample).contains("MMMAIL_OTEL_ENABLED=false", "OTEL_SERVICE_NAME=mmmail-server");
        assertThat(helmConfigMap).contains("MMMAIL_OTEL_ENABLED", "OTEL_EXPORTER_OTLP_ENDPOINT");
    }

    @Test
    void runtimeTraceServiceShouldEmitTaggedObservationsAndPropagateFailures() {
        ObservationRegistry registry = ObservationRegistry.create();
        List<StoppedObservation> stopped = new ArrayList<>();
        registry.observationConfig().observationHandler(new RecordingObservationHandler(stopped));
        RuntimeTraceService service = new RuntimeTraceService(registry);

        String result = service.observe("mmmail.billing.webhook", Map.of(
                "component", "billing",
                "provider", "webhook"
        ), () -> "ok");

        assertThat(result).isEqualTo("ok");
        assertThat(stopped).anySatisfy(observation -> {
            assertThat(observation.name()).isEqualTo("mmmail.billing.webhook");
            assertThat(observation.tag("component")).isEqualTo("billing");
            assertThat(observation.tag("provider")).isEqualTo("webhook");
            assertThat(observation.error()).isNull();
        });

        assertThatThrownBy(() -> service.observeVoid("mmmail.license.verify", Map.of(
                "component", "license"
        ), () -> {
            throw new IllegalStateException("invalid license");
        })).isInstanceOf(IllegalStateException.class).hasMessage("invalid license");

        assertThat(stopped).anySatisfy(observation -> {
            assertThat(observation.name()).isEqualTo("mmmail.license.verify");
            assertThat(observation.tag("component")).isEqualTo("license");
            assertThat(observation.error()).isInstanceOf(IllegalStateException.class);
        });
    }

    @Test
    void shouldWireRequiredRuntimeTraceSpanSurfaces() throws Exception {
        String requestFilter = readServerJava("observability/RequestTracingFilter.java");
        String billingWebhook = readServerJava("commercial/BillingWebhookService.java");
        String licenseSync = readServerJava("commercial/LicenseSyncService.java");
        String billingRepository = readServerJava("commercial/JdbcBillingWebhookEventRepository.java");
        String licenseRepository = readServerJava("commercial/JdbcLicenseStateRepository.java");
        String driveRateLimiter = readServerJava("service/DrivePublicShareRateLimiter.java");

        assertThat(requestFilter).contains("mmmail.http.request", "runtimeTraceService.start(");
        assertThat(billingWebhook).contains("mmmail.billing.webhook", "runtimeTraceService.observe(");
        assertThat(licenseSync).contains("mmmail.license.verify", "runtimeTraceService.observe(");
        assertThat(billingRepository).contains("mmmail.db.operation", "billing_webhook_event");
        assertThat(licenseRepository).contains("mmmail.db.operation", "license_state");
        assertThat(driveRateLimiter).contains("mmmail.redis.operation", "public_share_rate_limit");
    }

    @Test
    void shouldWireOpenTelemetryContractIntoLocalCiAndSpecGates() throws Exception {
        String validateLocal = read("scripts/validate-local.sh");
        String ci = read(".github/workflows/ci.yml");
        String spec = read("docs/v22-open-source-commercial-spec.md");
        String readme = read("README.md");

        assertThat(validateLocal).contains("BackendV22OpenTelemetryContractTest");
        assertThat(ci).contains("BackendV22OpenTelemetryContractTest");
        assertThat(spec).contains(
                "OBS-01 | partial done",
                "BackendV22OpenTelemetryContractTest",
                "OIDC callback 专项 span 随 BUS-01 OIDC 后端落地"
        );
        assertThat(readme).contains("docs/observability/opentelemetry.md");
    }

    private String readServerJava(String path) throws Exception {
        return read("backend/mmmail-server/src/main/java/com/mmmail/server/" + path);
    }

    private String read(String path) throws Exception {
        return Files.readString(repoRoot().resolve(path));
    }

    private Path repoRoot() {
        Path current = Path.of("").toAbsolutePath().normalize();
        while (current != null && !Files.exists(current.resolve(".github/workflows/ci.yml"))) {
            current = current.getParent();
        }
        assertThat(current).isNotNull();
        return current;
    }

    private record StoppedObservation(String name, Map<String, String> tags, Throwable error) {

        private String tag(String key) {
            return tags.get(key);
        }
    }

    private static final class RecordingObservationHandler implements ObservationHandler<Observation.Context> {

        private final List<StoppedObservation> stopped;

        private RecordingObservationHandler(List<StoppedObservation> stopped) {
            this.stopped = stopped;
        }

        @Override
        public boolean supportsContext(Observation.Context context) {
            return true;
        }

        @Override
        public void onStop(Observation.Context context) {
            stopped.add(new StoppedObservation(
                    context.getName(),
                    lowCardinalityTags(context),
                    context.getError()
            ));
        }

        private Map<String, String> lowCardinalityTags(Observation.Context context) {
            Map<String, String> tags = new java.util.LinkedHashMap<>();
            for (KeyValue keyValue : context.getLowCardinalityKeyValues()) {
                tags.put(keyValue.getKey(), keyValue.getValue());
            }
            return tags;
        }
    }
}
