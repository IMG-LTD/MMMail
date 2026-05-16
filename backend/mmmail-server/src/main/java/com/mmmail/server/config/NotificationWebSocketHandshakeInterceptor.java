package com.mmmail.server.config;

import com.mmmail.server.observability.WebSocketMetricsService;
import com.mmmail.server.security.JwtPrincipal;
import com.mmmail.server.security.WebSocketTokenAuthenticator;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Component
public class NotificationWebSocketHandshakeInterceptor implements HandshakeInterceptor {

    public static final String ATTR_PRINCIPAL = "notificationPrincipal";
    public static final String ATTR_SINCE = "notificationSince";
    private static final String MODULE_NOTIFICATIONS = "notifications";

    private final WebSocketTokenAuthenticator webSocketTokenAuthenticator;
    private final WebSocketAffinityCookieService webSocketAffinityCookieService;
    private final WebSocketMetricsService webSocketMetricsService;

    public NotificationWebSocketHandshakeInterceptor(
            WebSocketTokenAuthenticator webSocketTokenAuthenticator,
            WebSocketAffinityCookieService webSocketAffinityCookieService,
            WebSocketMetricsService webSocketMetricsService
    ) {
        this.webSocketTokenAuthenticator = webSocketTokenAuthenticator;
        this.webSocketAffinityCookieService = webSocketAffinityCookieService;
        this.webSocketMetricsService = webSocketMetricsService;
    }

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes
    ) {
        try {
            String token = queryParam(request, "token");
            JwtPrincipal principal = webSocketTokenAuthenticator.authenticate(token);
            attributes.put(ATTR_PRINCIPAL, principal);
            attributes.put(ATTR_SINCE, parseSince(queryParam(request, "since")));
            webSocketAffinityCookieService.attach(response);
            return true;
        } catch (RuntimeException exception) {
            int closeCode = WebSocketHandshakeFailureResponder.reject(response, exception);
            webSocketMetricsService.handshakeRejected(
                    MODULE_NOTIFICATIONS,
                    WebSocketHandshakeFailureResponder.metricReason(closeCode)
            );
            return false;
        }
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception
    ) {
    }

    private String queryParam(ServerHttpRequest request, String name) {
        return UriComponentsBuilder.fromUri(request.getURI()).build().getQueryParams().getFirst(name);
    }

    private Long parseSince(String value) {
        if (!StringUtils.hasText(value)) {
            return 0L;
        }
        long cursor = Long.parseLong(value);
        if (cursor < 0) {
            throw new IllegalArgumentException("notification websocket since cursor must be non-negative");
        }
        return cursor;
    }
}
