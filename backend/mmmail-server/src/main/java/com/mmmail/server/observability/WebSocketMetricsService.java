package com.mmmail.server.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class WebSocketMetricsService {

    private static final String UNKNOWN_TAG = "unknown";
    private static final String MODULE_NOTIFICATIONS = "notifications";

    private final MeterRegistry meterRegistry;
    private final ConcurrentMap<String, AtomicInteger> activeConnections = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, AtomicInteger> activeCollaborationRooms = new ConcurrentHashMap<>();

    public WebSocketMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void collaborationRoomOpened(String resourceType) {
        collaborationRoomCounter(resourceType).incrementAndGet();
    }

    public void collaborationRoomClosed(String resourceType) {
        collaborationRoomCounter(resourceType).updateAndGet(value -> Math.max(0, value - 1));
    }

    public void connectionOpened(String module) {
        activeCounter(module).incrementAndGet();
    }

    public void connectionClosed(String module, String reason) {
        activeCounter(module).updateAndGet(value -> Math.max(0, value - 1));
        disconnectCounter(module, reason).increment();
    }

    public void handshakeRejected(String module, String reason) {
        disconnectCounter(module, reason).increment();
    }

    private Counter disconnectCounter(String module, String reason) {
        return Counter.builder("ws_disconnect_total")
                .tag("module", tag(module))
                .tag("reason", tag(reason))
                .register(meterRegistry);
    }

    public void message(String module, String direction, String type, long latencyMs) {
        Counter.builder("ws_message_total")
                .tag("module", tag(module))
                .tag("direction", tag(direction))
                .tag("type", tag(type))
                .register(meterRegistry)
                .increment();
        Timer.builder("ws_message_latency_ms")
                .tag("module", tag(module))
                .publishPercentileHistogram()
                .register(meterRegistry)
                .record(Duration.ofMillis(Math.max(0L, latencyMs)));
    }

    public void notificationFanout(long latencyMs) {
        Timer.builder("ws_notification_fanout_latency_ms")
                .tag("module", MODULE_NOTIFICATIONS)
                .publishPercentileHistogram()
                .register(meterRegistry)
                .record(Duration.ofMillis(Math.max(0L, latencyMs)));
    }

    private AtomicInteger activeCounter(String module) {
        String normalizedModule = tag(module);
        return activeConnections.computeIfAbsent(normalizedModule, key -> {
            AtomicInteger counter = new AtomicInteger();
            Gauge.builder("ws_active_connections", counter, AtomicInteger::get)
                    .tag("module", key)
                    .register(meterRegistry);
            return counter;
        });
    }

    private AtomicInteger collaborationRoomCounter(String resourceType) {
        String normalizedType = tag(resourceType);
        return activeCollaborationRooms.computeIfAbsent(normalizedType, key -> {
            AtomicInteger counter = new AtomicInteger();
            Gauge.builder("ws_collab_active_rooms", counter, AtomicInteger::get)
                    .tag("resource_type", key)
                    .register(meterRegistry);
            return counter;
        });
    }

    private String tag(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN_TAG;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
