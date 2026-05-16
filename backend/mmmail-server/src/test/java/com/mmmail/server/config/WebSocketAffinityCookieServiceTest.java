package com.mmmail.server.config;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WebSocketAffinityCookieServiceTest {

    private static final long SPEC_COOKIE_MAX_AGE_SECONDS = Duration.ofDays(7).toSeconds();

    @Test
    void shouldBuildStickyAffinityCookie() {
        WebSocketAffinityCookieService service = new WebSocketAffinityCookieService(
                "WS_AFFINITY",
                "node-a",
                true,
                SPEC_COOKIE_MAX_AGE_SECONDS
        );

        String cookie = service.affinityCookie().toString();

        assertThat(cookie)
                .contains("WS_AFFINITY=node-a")
                .contains("Path=/ws")
                .contains("Max-Age=" + SPEC_COOKIE_MAX_AGE_SECONDS)
                .contains("HttpOnly")
                .contains("Secure")
                .contains("SameSite=Lax");
    }

    @Test
    void shouldRejectInvalidAffinityConfiguration() {
        assertThatThrownBy(() -> new WebSocketAffinityCookieService("", "node-a", true, SPEC_COOKIE_MAX_AGE_SECONDS))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cookie-name");
        assertThatThrownBy(() -> new WebSocketAffinityCookieService("WS_AFFINITY", "", true, SPEC_COOKIE_MAX_AGE_SECONDS))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("node-id");
        assertThatThrownBy(() -> new WebSocketAffinityCookieService("WS_AFFINITY", "node-a", true, 0L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cookie-max-age-seconds");
    }

    @Test
    void websocketHandshakeInterceptorsShouldAttachAffinityCookie() throws Exception {
        String notificationInterceptor = source("src/main/java/com/mmmail/server/config/NotificationWebSocketHandshakeInterceptor.java");
        String collabInterceptor = source("src/main/java/com/mmmail/server/config/CollabWebSocketHandshakeInterceptor.java");

        assertThat(notificationInterceptor).contains("webSocketAffinityCookieService.attach(response)");
        assertThat(collabInterceptor).contains("webSocketAffinityCookieService.attach(response)");
    }

    @Test
    void applicationPropertiesShouldExposeAffinityCookieSettings() throws Exception {
        String applicationYaml = source("src/main/resources/application.yml");
        String testYaml = source("src/test/resources/application-test.yml");

        assertThat(applicationYaml)
                .contains("cookie-name: ${MMMAIL_WEBSOCKET_AFFINITY_COOKIE_NAME:WS_AFFINITY}")
                .contains("node-id: ${MMMAIL_WEBSOCKET_AFFINITY_NODE_ID:mmmail-server}")
                .contains("cookie-secure: ${MMMAIL_WEBSOCKET_AFFINITY_COOKIE_SECURE:true}")
                .contains("cookie-max-age-seconds: ${MMMAIL_WEBSOCKET_AFFINITY_COOKIE_MAX_AGE_SECONDS:604800}");
        assertThat(testYaml)
                .contains("cookie-name: WS_AFFINITY")
                .contains("node-id: mmmail-test")
                .contains("cookie-secure: false")
                .contains("cookie-max-age-seconds: 604800");
    }

    private String source(String relativePath) throws Exception {
        return Files.readString(Path.of(relativePath));
    }
}
