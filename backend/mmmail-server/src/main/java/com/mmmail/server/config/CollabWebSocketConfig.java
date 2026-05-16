package com.mmmail.server.config;

import com.mmmail.server.controller.CollabWebSocketHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
public class CollabWebSocketConfig implements WebSocketConfigurer {

    private static final String COLLAB_WS_PATH = "/ws/collab/{resourceType}/{resourceId}";

    private final CollabWebSocketHandler collabWebSocketHandler;
    private final CollabWebSocketHandshakeInterceptor collabWebSocketHandshakeInterceptor;
    private final List<String> allowedOrigins;

    public CollabWebSocketConfig(
            CollabWebSocketHandler collabWebSocketHandler,
            CollabWebSocketHandshakeInterceptor collabWebSocketHandshakeInterceptor,
            @Value("${mmmail.cors-allowed-origins}") String corsAllowedOrigins
    ) {
        this.collabWebSocketHandler = collabWebSocketHandler;
        this.collabWebSocketHandshakeInterceptor = collabWebSocketHandshakeInterceptor;
        this.allowedOrigins = buildAllowedOrigins(corsAllowedOrigins);
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(collabWebSocketHandler, COLLAB_WS_PATH)
                .addInterceptors(collabWebSocketHandshakeInterceptor)
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
