package com.mmmail.server.observability;

import com.mmmail.common.observability.ErrorReporter;
import com.mmmail.common.observability.ObservedErrorReport;
import com.mmmail.server.model.dto.CreateClientErrorEventRequest;
import com.mmmail.server.model.vo.SystemHealthOverviewVo;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

@Service
public class ErrorTrackingService implements ErrorReporter {

    private static final int MAX_EVENTS = 100;
    private static final String SOURCE_CLIENT = "CLIENT";

    private final MeterRegistry meterRegistry;
    private final ArrayDeque<SystemHealthOverviewVo.ErrorEvent> recentEvents = new ArrayDeque<>();
    private final Object monitor = new Object();
    private final AtomicLong sequence = new AtomicLong(0);
    private final LongAdder totalEvents = new LongAdder();
    private final LongAdder serverEvents = new LongAdder();
    private final LongAdder clientEvents = new LongAdder();
    private volatile LocalDateTime lastOccurredAt;

    public ErrorTrackingService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        Gauge.builder("mmmail.errors.buffer.size", this, ErrorTrackingService::bufferSize).register(meterRegistry);
        Gauge.builder("mmmail.errors.total.events", this, ErrorTrackingService::totalCount).register(meterRegistry);
    }

    @Override
    public void record(ObservedErrorReport report) {
        SystemHealthOverviewVo.ErrorEvent event = toEvent(report);
        synchronized (monitor) {
            recentEvents.addFirst(event);
            trimBuffer();
        }
        totalEvents.increment();
        if (SOURCE_CLIENT.equalsIgnoreCase(report.source())) {
            clientEvents.increment();
        } else {
            serverEvents.increment();
        }
        lastOccurredAt = event.occurredAt();
        counter(report).increment();
    }

    public void recordClientError(
            Long userId,
            Long sessionId,
            String orgId,
            CreateClientErrorEventRequest request,
            String userAgent
    ) {
        record(new ObservedErrorReport(
                SOURCE_CLIENT,
                safeValue(request.category(), "CLIENT_RUNTIME"),
                safeValue(request.severity(), "ERROR"),
                request.path(),
                request.method(),
                null,
                null,
                request.message(),
                request.detail(),
                request.requestId(),
                stringify(userId),
                stringify(sessionId),
                orgId,
                userAgent,
                LocalDateTime.now()
        ));
    }

    public Summary summary() {
        return new Summary(totalCount(), serverEvents.sum(), clientEvents.sum(), lastOccurredAt);
    }

    public List<SystemHealthOverviewVo.ErrorEvent> recent(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, MAX_EVENTS));
        synchronized (monitor) {
            return recentEvents.stream().limit(safeLimit).toList();
        }
    }

    private Counter counter(ObservedErrorReport report) {
        return Counter.builder("mmmail.errors.events.total")
                .tag("source", safeValue(report.source(), "UNKNOWN"))
                .tag("category", safeValue(report.category(), "UNKNOWN"))
                .tag("severity", safeValue(report.severity(), "UNKNOWN"))
                .register(meterRegistry);
    }

    private SystemHealthOverviewVo.ErrorEvent toEvent(ObservedErrorReport report) {
        return new SystemHealthOverviewVo.ErrorEvent(
                String.valueOf(sequence.incrementAndGet()),
                report.source(),
                report.category(),
                report.severity(),
                report.message(),
                report.detail(),
                report.path(),
                report.method(),
                report.status(),
                report.errorCode(),
                report.requestId(),
                report.userId(),
                report.sessionId(),
                report.orgId(),
                report.occurredAt()
        );
    }

    private void trimBuffer() {
        while (recentEvents.size() > MAX_EVENTS) {
            recentEvents.removeLast();
        }
    }

    private int bufferSize() {
        synchronized (monitor) {
            return recentEvents.size();
        }
    }

    private long totalCount() {
        return totalEvents.sum();
    }

    private String safeValue(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private String stringify(Long value) {
        return value == null ? null : String.valueOf(value);
    }

    public record Summary(
            long totalEvents,
            long serverEvents,
            long clientEvents,
            LocalDateTime lastOccurredAt
    ) {
    }
}
