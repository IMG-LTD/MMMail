package com.mmmail.server.security;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WebSocketChannelLimitServiceTest {

    @Test
    void shouldRejectSubscriptionsBeyondConfiguredSessionLimit() {
        WebSocketChannelLimitService limitService = new WebSocketChannelLimitService(2, 750L);

        assertThat(limitService.subscribe("session-1", "notifications").allowed()).isTrue();
        assertThat(limitService.subscribe("session-1", "collab/docs/doc-1").allowed()).isTrue();

        WebSocketChannelLimitService.ChannelDecision denied = limitService.subscribe("session-1", "collab/sheets/sheet-1");

        assertThat(denied.allowed()).isFalse();
        assertThat(denied.retryAfterMs()).isEqualTo(750L);
    }

    @Test
    void shouldReleaseSessionSubscriptions() {
        WebSocketChannelLimitService limitService = new WebSocketChannelLimitService(1, 750L);
        assertThat(limitService.subscribe("session-1", "notifications").allowed()).isTrue();
        assertThat(limitService.subscribe("session-1", "collab/docs/doc-1").allowed()).isFalse();

        limitService.release("session-1");

        assertThat(limitService.subscribe("session-1", "collab/docs/doc-1").allowed()).isTrue();
    }

    @Test
    void shouldRejectInvalidSubscriptionLimitConfiguration() {
        assertThatThrownBy(() -> new WebSocketChannelLimitService(0, 750L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("max-channels-per-session");
        assertThatThrownBy(() -> new WebSocketChannelLimitService(1, 0L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("retry-after-ms");
    }

    @Test
    void websocketHandlersShouldEnforceSubscriptionLimit() throws Exception {
        String notificationHandler = source("controller/NotificationWebSocketHandler.java");
        String collabHandler = source("controller/CollabWebSocketHandler.java");

        assertThat(notificationHandler)
                .contains("webSocketChannelLimitService.subscribe")
                .contains("webSocketChannelLimitService.release");
        assertThat(collabHandler)
                .contains("webSocketChannelLimitService.subscribe")
                .contains("webSocketChannelLimitService.release");
    }

    @Test
    void applicationPropertiesShouldExposeSubscriptionLimitSettings() throws Exception {
        String applicationYaml = Files.readString(Path.of("src/main/resources/application.yml"));
        String testYaml = Files.readString(Path.of("src/test/resources/application-test.yml"));

        assertThat(applicationYaml)
                .contains("max-channels-per-session: ${MMMAIL_WEBSOCKET_SUBSCRIPTION_MAX_CHANNELS_PER_SESSION:32}")
                .contains("retry-after-ms: ${MMMAIL_WEBSOCKET_SUBSCRIPTION_RETRY_AFTER_MS:1000}");
        assertThat(testYaml)
                .contains("max-channels-per-session: 32")
                .contains("retry-after-ms: 1000");
    }

    private String source(String relativePath) throws Exception {
        return Files.readString(Path.of("src/main/java/com/mmmail/server").resolve(relativePath));
    }
}
