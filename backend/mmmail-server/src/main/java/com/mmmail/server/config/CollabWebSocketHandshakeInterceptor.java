package com.mmmail.server.config;

import com.mmmail.server.observability.WebSocketMetricsService;
import com.mmmail.server.security.JwtPrincipal;
import com.mmmail.server.security.WebSocketTokenAuthenticator;
import com.mmmail.server.service.CollabCrdtService;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Component
public class CollabWebSocketHandshakeInterceptor implements HandshakeInterceptor {

    public static final String ATTR_PRINCIPAL = "collabPrincipal";
    public static final String ATTR_RESOURCE_TYPE = "collabResourceType";
    public static final String ATTR_RESOURCE_ID = "collabResourceId";
    private static final String MODULE_COLLAB = "collab";

    private final WebSocketTokenAuthenticator webSocketTokenAuthenticator;
    private final CollabCrdtService collabCrdtService;
    private final WebSocketAffinityCookieService webSocketAffinityCookieService;
    private final WebSocketMetricsService webSocketMetricsService;

    public CollabWebSocketHandshakeInterceptor(
            WebSocketTokenAuthenticator webSocketTokenAuthenticator,
            CollabCrdtService collabCrdtService,
            WebSocketAffinityCookieService webSocketAffinityCookieService,
            WebSocketMetricsService webSocketMetricsService
    ) {
        this.webSocketTokenAuthenticator = webSocketTokenAuthenticator;
        this.collabCrdtService = collabCrdtService;
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
            JwtPrincipal principal = webSocketTokenAuthenticator.authenticate(queryParam(request, "token"));
            CollabPath path = parsePath(request.getURI().getPath());
            collabCrdtService.requireReadable(principal.userId(), path.resourceType(), path.resourceId());
            attributes.put(ATTR_PRINCIPAL, principal);
            attributes.put(ATTR_RESOURCE_TYPE, path.resourceType());
            attributes.put(ATTR_RESOURCE_ID, path.resourceId());
            webSocketAffinityCookieService.attach(response);
            return true;
        } catch (RuntimeException exception) {
            int closeCode = WebSocketHandshakeFailureResponder.reject(response, exception);
            webSocketMetricsService.handshakeRejected(
                    MODULE_COLLAB,
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

    private CollabPath parsePath(String path) {
        String[] segments = path.split("/");
        if (segments.length != 5 || !"ws".equals(segments[1]) || !"collab".equals(segments[2])) {
            throw new IllegalArgumentException("collaboration websocket path is invalid");
        }
        return new CollabPath(segments[3], segments[4]);
    }

    private record CollabPath(String resourceType, String resourceId) {
    }
}
