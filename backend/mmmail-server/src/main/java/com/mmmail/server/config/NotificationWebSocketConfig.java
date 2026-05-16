package com.mmmail.server.config;

import com.mmmail.server.controller.NotificationWebSocketHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSocket
public class NotificationWebSocketConfig implements WebSocketConfigurer {

    private static final String NOTIFICATION_WS_PATH = "/ws/notifications";

    private final NotificationWebSocketHandler notificationWebSocketHandler;
    private final NotificationWebSocketHandshakeInterceptor notificationWebSocketHandshakeInterceptor;
    private final List<String> allowedOrigins;

    public NotificationWebSocketConfig(
            NotificationWebSocketHandler notificationWebSocketHandler,
            NotificationWebSocketHandshakeInterceptor notificationWebSocketHandshakeInterceptor,
            @Value("${mmmail.cors-allowed-origins}") String corsAllowedOrigins
    ) {
        this.notificationWebSocketHandler = notificationWebSocketHandler;
        this.notificationWebSocketHandshakeInterceptor = notificationWebSocketHandshakeInterceptor;
        this.allowedOrigins = buildAllowedOrigins(corsAllowedOrigins);
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(notificationWebSocketHandler, NOTIFICATION_WS_PATH)
                .addInterceptors(notificationWebSocketHandshakeInterceptor)
                .setAllowedOriginPatterns(allowedOrigins.toArray(String[]::new));
    }

    private List<String> buildAllowedOrigins(String corsAllowedOrigins) {
        List<String> origins = new ArrayList<>(Arrays.stream(corsAllowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toList());
        origins.add("http://localhost:*");
        origins.add("http://127.0.0.1:*");
        return origins;
    }
}
