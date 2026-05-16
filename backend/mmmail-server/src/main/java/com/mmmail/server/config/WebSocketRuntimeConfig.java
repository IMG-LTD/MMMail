package com.mmmail.server.config;

import jakarta.servlet.ServletContext;
import jakarta.websocket.server.ServerContainer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.context.ServletWebServerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebSocketRuntimeConfig {

    private static final String DEFAULT_MAX_IDLE_TIMEOUT_MS = "300000";

    @Bean
    public ApplicationListener<ServletWebServerInitializedEvent> webSocketIdleTimeoutListener(
            @Value("${mmmail.websocket.session.max-idle-ms:" + DEFAULT_MAX_IDLE_TIMEOUT_MS + "}") long maxIdleTimeoutMs
    ) {
        long normalizedMaxIdleTimeoutMs = normalizeMaxIdleTimeoutMs(maxIdleTimeoutMs);
        return event -> applyMaxIdleTimeout(
                event.getApplicationContext().getServletContext(),
                normalizedMaxIdleTimeoutMs
        );
    }

    long normalizeMaxIdleTimeoutMs(long maxIdleTimeoutMs) {
        if (maxIdleTimeoutMs <= 0L) {
            throw new IllegalArgumentException("mmmail.websocket.session.max-idle-ms must be positive");
        }
        return maxIdleTimeoutMs;
    }

    void applyMaxIdleTimeout(ServletContext servletContext, long maxIdleTimeoutMs) {
        Object container = servletContext.getAttribute(ServerContainer.class.getName());
        if (container == null) {
            if (servletContext.getClass().getName().equals("org.springframework.mock.web.MockServletContext")) {
                return;
            }
            throw new IllegalStateException("Missing jakarta.websocket.server.ServerContainer servlet attribute");
        }
        if (!(container instanceof ServerContainer serverContainer)) {
            throw new IllegalStateException("Invalid jakarta.websocket.server.ServerContainer servlet attribute");
        }
        serverContainer.setDefaultMaxSessionIdleTimeout(normalizeMaxIdleTimeoutMs(maxIdleTimeoutMs));
    }
}
