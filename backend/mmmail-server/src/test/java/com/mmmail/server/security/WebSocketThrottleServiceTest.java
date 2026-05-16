package com.mmmail.server.security;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WebSocketThrottleServiceTest {

    @Test
    void shouldThrottleMessagesAfterConfiguredWindowLimit() {
        WebSocketThrottleService throttleService = new WebSocketThrottleService(3, 1, 250L);

        assertThat(throttleService.recordMessage("session-1").allowed()).isTrue();
        assertThat(throttleService.recordMessage("session-1").allowed()).isTrue();
        assertThat(throttleService.recordMessage("session-1").allowed()).isTrue();

        WebSocketThrottleService.ThrottleDecision denied = throttleService.recordMessage("session-1");

        assertThat(denied.allowed()).isFalse();
        assertThat(denied.retryAfterMs()).isEqualTo(250L);
    }

    @Test
    void shouldReleaseClosedSessionCounters() {
        WebSocketThrottleService throttleService = new WebSocketThrottleService(1, 1, 250L);
        throttleService.recordMessage("session-1");

        assertThat(throttleService.recordMessage("session-1").allowed()).isFalse();

        throttleService.release("session-1");

        assertThat(throttleService.recordMessage("session-1").allowed()).isTrue();
    }

    @Test
    void shouldRejectInvalidRateLimitConfiguration() {
        assertThatThrownBy(() -> new WebSocketThrottleService(0, 1, 250L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("max-messages-per-window");
        assertThatThrownBy(() -> new WebSocketThrottleService(1, 0, 250L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("window-seconds");
        assertThatThrownBy(() -> new WebSocketThrottleService(1, 1, 0L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("retry-after-ms");
    }

    @Test
    void websocketHandlersShouldSendThrottleFrameAndCloseWithSpecCode() throws Exception {
        String notificationHandler = source("controller/NotificationWebSocketHandler.java");
        String collabHandler = source("controller/CollabWebSocketHandler.java");

        assertThat(notificationHandler)
                .contains("webSocketThrottleService.recordMessage")
                .contains("\\\"type\\\":\\\"throttle\\\"")
                .contains("4429");
        assertThat(collabHandler)
                .contains("webSocketThrottleService.recordMessage")
                .contains("\\\"type\\\":\\\"throttle\\\"")
                .contains("4429");
    }

    private String source(String relativePath) throws Exception {
        return Files.readString(Path.of("src/main/java/com/mmmail/server").resolve(relativePath));
    }
}
