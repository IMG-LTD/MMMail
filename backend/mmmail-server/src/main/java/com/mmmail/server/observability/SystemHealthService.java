package com.mmmail.server.observability;

import com.mmmail.server.model.vo.SystemHealthOverviewVo;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.SystemHealth;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
public class SystemHealthService {

    private static final String PROMETHEUS_PATH = "/actuator/prometheus";

    private final Environment environment;
    private final HealthEndpoint healthEndpoint;
    private final MeterRegistry meterRegistry;
    private final RequestObservationService requestObservationService;
    private final ErrorTrackingService errorTrackingService;
    private final JobRunMonitorService jobRunMonitorService;
    private final String applicationName;
    private final String applicationVersion;

    public SystemHealthService(
            Environment environment,
            HealthEndpoint healthEndpoint,
            MeterRegistry meterRegistry,
            RequestObservationService requestObservationService,
            ErrorTrackingService errorTrackingService,
            JobRunMonitorService jobRunMonitorService,
            @Value("${spring.application.name:mmmail-server}") String applicationName,
            @Value("${mmmail.observability.app-version:0.1.0-SNAPSHOT}") String applicationVersion
    ) {
        this.environment = environment;
        this.healthEndpoint = healthEndpoint;
        this.meterRegistry = meterRegistry;
        this.requestObservationService = requestObservationService;
        this.errorTrackingService = errorTrackingService;
        this.jobRunMonitorService = jobRunMonitorService;
        this.applicationName = applicationName;
        this.applicationVersion = applicationVersion;
    }

    public SystemHealthOverviewVo getOverview() {
        HealthComponent healthComponent = healthEndpoint.health();
        List<SystemHealthOverviewVo.ComponentStatus> components = toComponents(healthComponent);
        RequestObservationService.Summary requestSummary = requestObservationService.snapshot();
        ErrorTrackingService.Summary errorSummary = errorTrackingService.summary();
        JobRunMonitorService.Summary jobSummary = jobRunMonitorService.summary();
        return new SystemHealthOverviewVo(
                deriveStatus(healthComponent, components),
                applicationName,
                applicationVersion,
                activeProfiles(),
                uptimeSeconds(),
                LocalDateTime.now(),
                components,
                metricSummary(requestSummary),
                new SystemHealthOverviewVo.ErrorTrackingSummary(
                        errorSummary.totalEvents(),
                        errorSummary.serverEvents(),
                        errorSummary.clientEvents(),
                        errorSummary.lastOccurredAt()
                ),
                errorTrackingService.recent(10),
                new SystemHealthOverviewVo.JobSummary(
                        jobSummary.activeRuns(),
                        jobSummary.totalRuns(),
                        jobSummary.failedRuns(),
                        jobSummary.lastCompletedAt()
                ),
                jobRunMonitorService.recent(10),
                PROMETHEUS_PATH
        );
    }

    private List<String> activeProfiles() {
        String[] activeProfiles = environment.getActiveProfiles();
        if (activeProfiles.length == 0) {
            return Arrays.asList(environment.getDefaultProfiles());
        }
        return Arrays.asList(activeProfiles);
    }

    private Long uptimeSeconds() {
        return ManagementFactory.getRuntimeMXBean().getUptime() / 1000;
    }

    private String deriveStatus(HealthComponent healthComponent, List<SystemHealthOverviewVo.ComponentStatus> components) {
        String baseStatus = healthComponent.getStatus().getCode();
        boolean anyDown = components.stream().anyMatch(item -> "DOWN".equalsIgnoreCase(item.status()) || "OUT_OF_SERVICE".equalsIgnoreCase(item.status()));
        boolean anyWarning = components.stream().anyMatch(item -> !"UP".equalsIgnoreCase(item.status()));
        if (anyDown) {
            return "DOWN";
        }
        if (anyWarning || !"UP".equalsIgnoreCase(baseStatus)) {
            return "DEGRADED";
        }
        return "UP";
    }

    private List<SystemHealthOverviewVo.ComponentStatus> toComponents(HealthComponent healthComponent) {
        if (healthComponent instanceof SystemHealth systemHealth) {
            return systemHealth.getComponents().entrySet().stream()
                    .map(entry -> toComponent(entry.getKey(), entry.getValue()))
                    .sorted(java.util.Comparator.comparing(SystemHealthOverviewVo.ComponentStatus::name))
                    .toList();
        }
        return List.of(toComponent("application", healthComponent));
    }

    private SystemHealthOverviewVo.ComponentStatus toComponent(String name, HealthComponent component) {
        if (component instanceof Health health) {
            return new SystemHealthOverviewVo.ComponentStatus(name, health.getStatus().getCode(), flattenDetails(health.getDetails()));
        }
        return new SystemHealthOverviewVo.ComponentStatus(name, component.getStatus().getCode(), "");
    }

    private String flattenDetails(Map<String, Object> details) {
        if (details == null || details.isEmpty()) {
            return "";
        }
        return details.entrySet().stream()
                .limit(4)
                .map(entry -> entry.getKey() + "=" + String.valueOf(entry.getValue()))
                .reduce((left, right) -> left + ", " + right)
                .orElse("");
    }

    private SystemHealthOverviewVo.MetricSummary metricSummary(RequestObservationService.Summary requestSummary) {
        return new SystemHealthOverviewVo.MetricSummary(
                requestSummary.totalRequests(),
                requestSummary.failedRequests(),
                percentageGauge("process.cpu.usage"),
                percentageGauge("system.cpu.usage"),
                bytesToMb(gaugeValue("jvm.memory.used")),
                bytesToMb(gaugeValue("jvm.memory.max")),
                gaugeValue("jvm.threads.live"),
                gaugeValue("jdbc.connections.active"),
                gaugeValue("jdbc.connections.max"),
                requestSummary.modules().stream()
                        .map(item -> new SystemHealthOverviewVo.RequestMetric(item.module(), item.totalRequests(), item.failedRequests()))
                        .toList()
        );
    }

    private Double percentageGauge(String name) {
        Double value = gaugeValue(name);
        return value == null ? null : value * 100;
    }

    private Double bytesToMb(Double bytes) {
        return bytes == null ? null : bytes / 1024 / 1024;
    }

    private Double gaugeValue(String name) {
        Gauge gauge = meterRegistry.find(name).gauge();
        return gauge == null ? null : gauge.value();
    }
}
