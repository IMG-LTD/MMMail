package com.mmmail.server.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;

@Service
public class WebSocketAffinityCookieService {

    private static final String COOKIE_PATH = "/ws";
    private static final String SAME_SITE_LAX = "Lax";

    private final String cookieName;
    private final String nodeId;
    private final boolean cookieSecure;
    private final long cookieMaxAgeSeconds;

    public WebSocketAffinityCookieService(
            @Value("${mmmail.websocket.affinity.cookie-name:WS_AFFINITY}") String cookieName,
            @Value("${mmmail.websocket.affinity.node-id:mmmail-server}") String nodeId,
            @Value("${mmmail.websocket.affinity.cookie-secure:true}") boolean cookieSecure,
            @Value("${mmmail.websocket.affinity.cookie-max-age-seconds:604800}") long cookieMaxAgeSeconds
    ) {
        this.cookieName = requireText("mmmail.websocket.affinity.cookie-name", cookieName);
        this.nodeId = requireText("mmmail.websocket.affinity.node-id", nodeId);
        this.cookieSecure = cookieSecure;
        this.cookieMaxAgeSeconds = requirePositive(
                "mmmail.websocket.affinity.cookie-max-age-seconds",
                cookieMaxAgeSeconds
        );
    }

    public void attach(ServerHttpResponse response) {
        response.getHeaders().add(HttpHeaders.SET_COOKIE, affinityCookie().toString());
    }

    ResponseCookie affinityCookie() {
        return ResponseCookie.from(cookieName, nodeId)
                .path(COOKIE_PATH)
                .maxAge(Duration.ofSeconds(cookieMaxAgeSeconds))
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(SAME_SITE_LAX)
                .build();
    }

    private String requireText(String propertyName, String value) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(propertyName + " must not be blank");
        }
        return value.trim();
    }

    private long requirePositive(String propertyName, long value) {
        if (value <= 0L) {
            throw new IllegalArgumentException(propertyName + " must be positive");
        }
        return value;
    }
}
