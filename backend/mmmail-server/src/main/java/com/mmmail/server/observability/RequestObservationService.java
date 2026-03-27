package com.mmmail.server.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.LongAdder;

@Service
public class RequestObservationService {

    private final MeterRegistry meterRegistry;
    private final ConcurrentMap<String, ModuleCounters> moduleCounters = new ConcurrentHashMap<>();

    public RequestObservationService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void record(RequestObservation observation) {
        ModuleCounters counters = moduleCounters.computeIfAbsent(observation.module(), key -> new ModuleCounters());
        counters.total().increment();
        counter("mmmail.api.requests.total", observation, statusFamily(observation.status())).increment();
        timer(observation).record(Duration.ofMillis(observation.durationMs()));
        if (observation.status() >= 400) {
            counters.failed().increment();
            counter("mmmail.api.requests.failed.total", observation, statusFamily(observation.status())).increment();
        }
    }

    public Summary snapshot() {
        List<ModuleMetric> modules = moduleCounters.entrySet().stream()
                .map(entry -> new ModuleMetric(entry.getKey(), entry.getValue().total().sum(), entry.getValue().failed().sum()))
                .sorted(Comparator.comparing(ModuleMetric::module))
                .toList();
        long totalRequests = modules.stream().mapToLong(ModuleMetric::totalRequests).sum();
        long failedRequests = modules.stream().mapToLong(ModuleMetric::failedRequests).sum();
        return new Summary(totalRequests, failedRequests, modules);
    }

    private Counter counter(String name, RequestObservation observation, String statusFamily) {
        return Counter.builder(name)
                .tag("module", observation.module())
                .tag("method", observation.method())
                .tag("status_family", statusFamily)
                .register(meterRegistry);
    }

    private Timer timer(RequestObservation observation) {
        return Timer.builder("mmmail.api.request.duration")
                .tag("module", observation.module())
                .tag("method", observation.method())
                .register(meterRegistry);
    }

    private String statusFamily(int status) {
        int family = Math.max(1, status / 100);
        return family + "xx";
    }

    private record ModuleCounters(LongAdder total, LongAdder failed) {

        private ModuleCounters() {
            this(new LongAdder(), new LongAdder());
        }
    }

    public record RequestObservation(
            String module,
            String method,
            int status,
            long durationMs
    ) {
    }

    public record Summary(
            long totalRequests,
            long failedRequests,
            List<ModuleMetric> modules
    ) {
    }

    public record ModuleMetric(
            String module,
            long totalRequests,
            long failedRequests
    ) {
    }
}
