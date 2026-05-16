package com.mmmail.server.observability;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class WebSocketMetricsServiceTest {

    @Test
    void shouldExposeRequiredWsGatewayMetrics() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        WebSocketMetricsService metrics = new WebSocketMetricsService(registry);

        metrics.connectionOpened("notifications");
        metrics.message("notifications", "down", "notification", 12L);
        metrics.connectionClosed("notifications", "client");

        assertThat(registry.get("ws_active_connections")
                .tag("module", "notifications")
                .gauge()
                .value()).isZero();
        assertThat(registry.get("ws_message_total")
                .tag("module", "notifications")
                .tag("direction", "down")
                .tag("type", "notification")
                .counter()
                .count()).isEqualTo(1.0);
        assertThat(registry.get("ws_message_latency_ms")
                .tag("module", "notifications")
                .timer()
                .count()).isEqualTo(1);
        assertThat(registry.get("ws_disconnect_total")
                .tag("module", "notifications")
                .tag("reason", "client")
                .counter()
                .count()).isEqualTo(1.0);
    }

    @Test
    void shouldExposeCollaborationRoomAndNotificationFanoutMetrics() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        WebSocketMetricsService metrics = new WebSocketMetricsService(registry);

        metrics.collaborationRoomOpened("docs");
        metrics.collaborationRoomOpened("docs");
        metrics.collaborationRoomClosed("docs");
        metrics.notificationFanout(42L);

        assertThat(registry.get("ws_collab_active_rooms")
                .tag("resource_type", "docs")
                .gauge()
                .value()).isEqualTo(1.0);
        assertThat(registry.get("ws_notification_fanout_latency_ms")
                .tag("module", "notifications")
                .timer()
                .count()).isEqualTo(1);
    }

    @Test
    void grafanaDashboardShouldTrackWsGatewayMetrics() throws Exception {
        String dashboard = Files.readString(Path.of("..", "..", "ops", "grafana", "ws-gateway-dashboard.json"));

        assertThat(dashboard).contains(
                "ws_active_connections",
                "ws_message_total",
                "ws_message_latency_ms",
                "ws_disconnect_total",
                "ws_collab_active_rooms",
                "ws_notification_fanout_latency_ms"
        );
    }

    @Test
    void runtimeCodeShouldRecordDashboardSpecificMetrics() throws Exception {
        String collabHandler = Files.readString(Path.of(
                "src/main/java/com/mmmail/server/controller/CollabWebSocketHandler.java"
        ));
        String notificationService = Files.readString(Path.of(
                "src/main/java/com/mmmail/server/service/NotificationRealtimeService.java"
        ));

        assertThat(collabHandler).contains("collaborationRoomOpened", "collaborationRoomClosed");
        assertThat(notificationService).contains("notificationFanout");
    }
}
