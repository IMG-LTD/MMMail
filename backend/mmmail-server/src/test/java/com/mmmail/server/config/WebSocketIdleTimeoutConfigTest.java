package com.mmmail.server.config;

import jakarta.websocket.server.ServerContainer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.boot.web.servlet.context.ServletWebServerInitializedEvent;
import org.springframework.mock.web.MockServletContext;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebSocketIdleTimeoutConfigTest {

    private static final long SPEC_IDLE_TIMEOUT_MS = Duration.ofMinutes(5).toMillis();

    @Test
    void shouldRejectInvalidIdleTimeoutExplicitly() {
        WebSocketRuntimeConfig config = new WebSocketRuntimeConfig();

        assertThatThrownBy(() -> config.normalizeMaxIdleTimeoutMs(0L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("max-idle-ms");
        assertThatThrownBy(() -> config.normalizeMaxIdleTimeoutMs(-1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("max-idle-ms");
        assertThat(config.normalizeMaxIdleTimeoutMs(Duration.ofMinutes(2).toMillis()))
                .isEqualTo(Duration.ofMinutes(2).toMillis());
    }

    @Test
    void shouldConfigureServletContainerIdleTimeoutFromApplicationProperties() throws Exception {
        String configSource = source("src/main/java/com/mmmail/server/config/WebSocketRuntimeConfig.java");
        String applicationYaml = source("src/main/resources/application.yml");
        String testYaml = source("src/test/resources/application-test.yml");

        assertThat(configSource)
                .contains("ApplicationListener<ServletWebServerInitializedEvent>")
                .contains("setDefaultMaxSessionIdleTimeout")
                .contains("mmmail.websocket.session.max-idle-ms")
                .doesNotContain("ServletContextInitializer");
        assertThat(applicationYaml)
                .contains("max-idle-ms: ${MMMAIL_WEBSOCKET_SESSION_MAX_IDLE_MS:300000}");
        assertThat(testYaml)
                .contains("max-idle-ms: 300000");
    }

    @Test
    void shouldApplyIdleTimeoutWhenServletContainerExists() {
        WebSocketRuntimeConfig config = new WebSocketRuntimeConfig();
        MockServletContext servletContext = new MockServletContext();
        ServerContainer serverContainer = mock(ServerContainer.class);
        servletContext.setAttribute(ServerContainer.class.getName(), serverContainer);

        config.applyMaxIdleTimeout(servletContext, SPEC_IDLE_TIMEOUT_MS);

        verify(serverContainer).setDefaultMaxSessionIdleTimeout(SPEC_IDLE_TIMEOUT_MS);
    }

    @Test
    void shouldApplyIdleTimeoutAfterServletWebServerStarts() {
        WebSocketRuntimeConfig config = new WebSocketRuntimeConfig();
        MockServletContext servletContext = new MockServletContext();
        ServerContainer serverContainer = mock(ServerContainer.class);
        ServletWebServerApplicationContext context = mock(ServletWebServerApplicationContext.class);
        ServletWebServerInitializedEvent event = new ServletWebServerInitializedEvent(mock(WebServer.class), context);
        servletContext.setAttribute(ServerContainer.class.getName(), serverContainer);
        when(context.getServletContext()).thenReturn(servletContext);

        config.webSocketIdleTimeoutListener(SPEC_IDLE_TIMEOUT_MS).onApplicationEvent(event);

        verify(serverContainer).setDefaultMaxSessionIdleTimeout(SPEC_IDLE_TIMEOUT_MS);
    }

    @Test
    void shouldSkipMockServletContextWithoutCreatingFailingBean() {
        WebSocketRuntimeConfig config = new WebSocketRuntimeConfig();

        config.applyMaxIdleTimeout(new MockServletContext(), SPEC_IDLE_TIMEOUT_MS);
    }

    private String source(String relativePath) throws Exception {
        return Files.readString(Path.of(relativePath));
    }
}
