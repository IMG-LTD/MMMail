package com.mmmail.server.security;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WebSocketConnectionLimitServiceTest {

    @Test
    void shouldRejectConnectionsBeyondConfiguredGlobalLimit() {
        WebSocketConnectionLimitService limitService = new WebSocketConnectionLimitService(2, 500L);

        assertThat(limitService.open("session-1").allowed()).isTrue();
        assertThat(limitService.open("session-2").allowed()).isTrue();

        WebSocketConnectionLimitService.ConnectionDecision denied = limitService.open("session-3");

        assertThat(denied.allowed()).isFalse();
        assertThat(denied.retryAfterMs()).isEqualTo(500L);
    }

    @Test
    void shouldReleaseClosedConnectionsFromGlobalLimit() {
        WebSocketConnectionLimitService limitService = new WebSocketConnectionLimitService(1, 500L);
        assertThat(limitService.open("session-1").allowed()).isTrue();
        assertThat(limitService.open("session-2").allowed()).isFalse();

        limitService.release("session-1");

        assertThat(limitService.open("session-2").allowed()).isTrue();
    }

    @Test
    void shouldRejectInvalidConnectionLimitConfiguration() {
        assertThatThrownBy(() -> new WebSocketConnectionLimitService(0, 500L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("max-active");
        assertThatThrownBy(() -> new WebSocketConnectionLimitService(1, 0L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("retry-after-ms");
    }

    @Test
    void websocketHandlersShouldEnforceGlobalConnectionLimitBeforeRegisteringSession() throws Exception {
        String notificationHandler = source("controller/NotificationWebSocketHandler.java");
        String collabHandler = source("controller/CollabWebSocketHandler.java");

        assertThat(notificationHandler)
                .contains("webSocketConnectionLimitService.open")
                .contains("rejectConnectionIfNeeded")
                .contains("webSocketConnectionLimitService.release");
        assertThat(collabHandler)
                .contains("webSocketConnectionLimitService.open")
                .contains("rejectConnectionIfNeeded")
                .contains("webSocketConnectionLimitService.release");
    }

    private String source(String relativePath) throws Exception {
        return Files.readString(Path.of("src/main/java/com/mmmail/server").resolve(relativePath));
    }
}
