package com.mmmail.server.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmmail.server.config.NotificationWebSocketHandshakeInterceptor;
import com.mmmail.server.model.vo.SuiteNotificationSyncVo;
import com.mmmail.server.observability.WebSocketMetricsService;
import com.mmmail.server.security.JwtPrincipal;
import com.mmmail.server.security.WebSocketChannelLimitService;
import com.mmmail.server.security.WebSocketConnectionLimitService;
import com.mmmail.server.security.WebSocketThrottleService;
import com.mmmail.server.service.NotificationRealtimeService;
import com.mmmail.server.service.SuiteNotificationSyncService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    private static final String TYPE_PING = "ping";
    private static final String TYPE_UNSUPPORTED = "unsupported";
    private static final String THROTTLE_FRAME_TEMPLATE = "{\"type\":\"throttle\",\"retryAfterMs\":%d}";
    private static final String MODULE_NOTIFICATIONS = "notifications";
    private static final String DIRECTION_UP = "up";
    private static final String REASON_CLIENT = "client";
    private static final String REASON_SERVER = "server";
    private static final String REASON_THROTTLE = "throttle";
    private static final CloseStatus THROTTLED = new CloseStatus(4429, "websocket message rate limit exceeded");

    private final ObjectMapper objectMapper;
    private final SuiteNotificationSyncService suiteNotificationSyncService;
    private final NotificationRealtimeService notificationRealtimeService;
    private final WebSocketMetricsService webSocketMetricsService;
    private final WebSocketChannelLimitService webSocketChannelLimitService;
    private final WebSocketConnectionLimitService webSocketConnectionLimitService;
    private final WebSocketThrottleService webSocketThrottleService;

    public NotificationWebSocketHandler(
            ObjectMapper objectMapper,
            SuiteNotificationSyncService suiteNotificationSyncService,
            NotificationRealtimeService notificationRealtimeService,
            WebSocketMetricsService webSocketMetricsService,
            WebSocketChannelLimitService webSocketChannelLimitService,
            WebSocketConnectionLimitService webSocketConnectionLimitService,
            WebSocketThrottleService webSocketThrottleService
    ) {
        this.objectMapper = objectMapper;
        this.suiteNotificationSyncService = suiteNotificationSyncService;
        this.notificationRealtimeService = notificationRealtimeService;
        this.webSocketMetricsService = webSocketMetricsService;
        this.webSocketChannelLimitService = webSocketChannelLimitService;
        this.webSocketConnectionLimitService = webSocketConnectionLimitService;
        this.webSocketThrottleService = webSocketThrottleService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        if (rejectConnectionIfNeeded(session)) {
            return;
        }
        if (rejectSubscriptionIfNeeded(session, MODULE_NOTIFICATIONS)) {
            return;
        }
        JwtPrincipal principal = principal(session);
        Long since = since(session);
        notificationRealtimeService.register(principal.userId(), session);
        webSocketMetricsService.connectionOpened(MODULE_NOTIFICATIONS);
        SuiteNotificationSyncVo replay = suiteNotificationSyncService.getNotificationWebSocketReplay(principal.userId(), since);
        notificationRealtimeService.sendReplay(session, principal.userId(), replay);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        if (throttleIfNeeded(session)) {
            return;
        }
        long started = System.nanoTime();
        JsonNode frame = objectMapper.readTree(message.getPayload());
        if (TYPE_PING.equals(frame.path("type").asText())) {
            recordInbound(TYPE_PING, started);
            notificationRealtimeService.sendPong(session, principal(session).userId());
            return;
        }
        recordInbound(TYPE_UNSUPPORTED, started);
        session.close(CloseStatus.NOT_ACCEPTABLE.withReason("unsupported notification websocket frame type"));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        JwtPrincipal principal = principal(session);
        notificationRealtimeService.unregister(principal.userId(), session);
        webSocketChannelLimitService.release(session.getId());
        webSocketConnectionLimitService.release(session.getId());
        webSocketThrottleService.release(session.getId());
        webSocketMetricsService.connectionClosed(MODULE_NOTIFICATIONS, closeReason(status));
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        JwtPrincipal principal = principal(session);
        notificationRealtimeService.unregister(principal.userId(), session);
        webSocketChannelLimitService.release(session.getId());
        webSocketConnectionLimitService.release(session.getId());
        webSocketThrottleService.release(session.getId());
        session.close(CloseStatus.SERVER_ERROR.withReason("notification websocket transport error"));
    }

    private boolean rejectConnectionIfNeeded(WebSocketSession session) throws java.io.IOException {
        WebSocketConnectionLimitService.ConnectionDecision decision = webSocketConnectionLimitService.open(session.getId());
        if (decision.allowed()) {
            return false;
        }
        session.sendMessage(new TextMessage(THROTTLE_FRAME_TEMPLATE.formatted(decision.retryAfterMs())));
        session.close(THROTTLED);
        return true;
    }

    private boolean rejectSubscriptionIfNeeded(WebSocketSession session, String channel) throws java.io.IOException {
        WebSocketChannelLimitService.ChannelDecision decision = webSocketChannelLimitService.subscribe(session.getId(), channel);
        if (decision.allowed()) {
            return false;
        }
        webSocketConnectionLimitService.release(session.getId());
        session.sendMessage(new TextMessage(THROTTLE_FRAME_TEMPLATE.formatted(decision.retryAfterMs())));
        session.close(THROTTLED);
        return true;
    }

    private boolean throttleIfNeeded(WebSocketSession session) throws java.io.IOException {
        WebSocketThrottleService.ThrottleDecision decision = webSocketThrottleService.recordMessage(session.getId());
        if (decision.allowed()) {
            return false;
        }
        session.sendMessage(new TextMessage(THROTTLE_FRAME_TEMPLATE.formatted(decision.retryAfterMs())));
        session.close(THROTTLED);
        return true;
    }

    private void recordInbound(String type, long startedNanos) {
        long elapsedMs = java.time.Duration.ofNanos(System.nanoTime() - startedNanos).toMillis();
        webSocketMetricsService.message(MODULE_NOTIFICATIONS, DIRECTION_UP, type, elapsedMs);
    }

    private String closeReason(CloseStatus status) {
        if (status.getCode() == THROTTLED.getCode()) {
            return REASON_THROTTLE;
        }
        return status.getCode() == CloseStatus.NORMAL.getCode() ? REASON_CLIENT : REASON_SERVER;
    }

    private JwtPrincipal principal(WebSocketSession session) {
        Object principal = session.getAttributes().get(NotificationWebSocketHandshakeInterceptor.ATTR_PRINCIPAL);
        if (principal instanceof JwtPrincipal jwtPrincipal) {
            return jwtPrincipal;
        }
        throw new IllegalStateException("notification websocket principal is required");
    }

    private Long since(WebSocketSession session) {
        Object since = session.getAttributes().get(NotificationWebSocketHandshakeInterceptor.ATTR_SINCE);
        return since instanceof Long cursor ? cursor : 0L;
    }
}
