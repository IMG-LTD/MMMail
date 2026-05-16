package com.mmmail.server.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmmail.server.config.CollabWebSocketHandshakeInterceptor;
import com.mmmail.server.observability.WebSocketMetricsService;
import com.mmmail.server.security.JwtPrincipal;
import com.mmmail.server.security.WebSocketChannelLimitService;
import com.mmmail.server.security.WebSocketConnectionLimitService;
import com.mmmail.server.security.WebSocketThrottleService;
import com.mmmail.server.service.CollabCrdtService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.io.IOException;
import java.time.Duration;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class CollabWebSocketHandler extends AbstractWebSocketHandler {

    private static final CloseStatus UNSUPPORTED_FRAME = CloseStatus.NOT_ACCEPTABLE.withReason("unsupported collab frame");
    private static final String TYPE_PING = "ping";
    private static final String TYPE_PONG = "pong";
    private static final String TYPE_UNSUPPORTED = "unsupported";
    private static final String TYPE_UPDATE = "update";
    private static final String THROTTLE_FRAME_TEMPLATE = "{\"type\":\"throttle\",\"retryAfterMs\":%d}";
    private static final String MODULE_COLLAB = "collab";
    private static final String DIRECTION_DOWN = "down";
    private static final String DIRECTION_UP = "up";
    private static final String REASON_CLIENT = "client";
    private static final String REASON_SERVER = "server";
    private static final String REASON_THROTTLE = "throttle";
    private static final long CONTROL_FRAME_SEQ = 0L;
    private static final CloseStatus THROTTLED = new CloseStatus(4429, "websocket message rate limit exceeded");

    private final CollabCrdtService collabCrdtService;
    private final ObjectMapper objectMapper;
    private final WebSocketMetricsService webSocketMetricsService;
    private final WebSocketChannelLimitService webSocketChannelLimitService;
    private final WebSocketConnectionLimitService webSocketConnectionLimitService;
    private final WebSocketThrottleService webSocketThrottleService;
    private final ConcurrentHashMap<String, CopyOnWriteArrayList<WebSocketSession>> sessions = new ConcurrentHashMap<>();

    public CollabWebSocketHandler(
            CollabCrdtService collabCrdtService,
            ObjectMapper objectMapper,
            WebSocketMetricsService webSocketMetricsService,
            WebSocketChannelLimitService webSocketChannelLimitService,
            WebSocketConnectionLimitService webSocketConnectionLimitService,
            WebSocketThrottleService webSocketThrottleService
    ) {
        this.collabCrdtService = collabCrdtService;
        this.objectMapper = objectMapper;
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
        if (rejectSubscriptionIfNeeded(session, channel(session))) {
            return;
        }
        register(session);
        replayStoredUpdates(session);
        webSocketMetricsService.connectionOpened(MODULE_COLLAB);
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        if (throttleIfNeeded(session)) {
            return;
        }
        long started = System.nanoTime();
        byte[] payload = readPayload(message.getPayload());
        JwtPrincipal principal = principal(session);
        collabCrdtService.appendUpdate(principal.userId(), resourceType(session), resourceId(session), payload);
        broadcast(session, payload);
        recordMessage(DIRECTION_UP, TYPE_UPDATE, started);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        if (throttleIfNeeded(session)) {
            return;
        }
        long started = System.nanoTime();
        JsonNode frame = objectMapper.readTree(message.getPayload());
        if (TYPE_PING.equals(frame.path("type").asText())) {
            session.sendMessage(new TextMessage(pongFrame(session)));
            recordMessage(DIRECTION_UP, TYPE_PING, started);
            recordMessage(DIRECTION_DOWN, TYPE_PONG, started);
            return;
        }
        recordMessage(DIRECTION_UP, TYPE_UNSUPPORTED, started);
        session.close(UNSUPPORTED_FRAME);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        unregister(session);
        webSocketChannelLimitService.release(session.getId());
        webSocketConnectionLimitService.release(session.getId());
        webSocketThrottleService.release(session.getId());
        webSocketMetricsService.connectionClosed(MODULE_COLLAB, closeReason(status));
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        unregister(session);
        webSocketChannelLimitService.release(session.getId());
        webSocketConnectionLimitService.release(session.getId());
        webSocketThrottleService.release(session.getId());
        session.close(CloseStatus.SERVER_ERROR.withReason("collab websocket transport error"));
    }

    private boolean rejectConnectionIfNeeded(WebSocketSession session) throws IOException {
        WebSocketConnectionLimitService.ConnectionDecision decision = webSocketConnectionLimitService.open(session.getId());
        if (decision.allowed()) {
            return false;
        }
        session.sendMessage(new TextMessage(THROTTLE_FRAME_TEMPLATE.formatted(decision.retryAfterMs())));
        session.close(THROTTLED);
        return true;
    }

    private boolean rejectSubscriptionIfNeeded(WebSocketSession session, String channel) throws IOException {
        WebSocketChannelLimitService.ChannelDecision decision = webSocketChannelLimitService.subscribe(session.getId(), channel);
        if (decision.allowed()) {
            return false;
        }
        webSocketConnectionLimitService.release(session.getId());
        session.sendMessage(new TextMessage(THROTTLE_FRAME_TEMPLATE.formatted(decision.retryAfterMs())));
        session.close(THROTTLED);
        return true;
    }

    private boolean throttleIfNeeded(WebSocketSession session) throws IOException {
        WebSocketThrottleService.ThrottleDecision decision = webSocketThrottleService.recordMessage(session.getId());
        if (decision.allowed()) {
            return false;
        }
        session.sendMessage(new TextMessage(THROTTLE_FRAME_TEMPLATE.formatted(decision.retryAfterMs())));
        session.close(THROTTLED);
        return true;
    }

    private void broadcast(WebSocketSession sender, byte[] payload) {
        for (WebSocketSession session : sessions.getOrDefault(channel(sender), new CopyOnWriteArrayList<>())) {
            sendToPeer(sender, session, payload);
        }
    }

    private void sendToPeer(WebSocketSession sender, WebSocketSession session, byte[] payload) {
        if (!session.isOpen() || session.getId().equals(sender.getId())) {
            return;
        }
        try {
            long started = System.nanoTime();
            session.sendMessage(new BinaryMessage(payload));
            recordMessage(DIRECTION_DOWN, TYPE_UPDATE, started);
        } catch (IOException exception) {
            unregister(session);
        }
    }

    private void unregister(WebSocketSession session) {
        sessions.computeIfPresent(channel(session), (ignored, channelSessions) -> {
            channelSessions.remove(session);
            if (!channelSessions.isEmpty()) {
                return channelSessions;
            }
            webSocketMetricsService.collaborationRoomClosed(resourceType(session));
            return null;
        });
    }

    private void register(WebSocketSession session) {
        sessions.compute(channel(session), (ignored, channelSessions) -> {
            CopyOnWriteArrayList<WebSocketSession> current = channelSessions == null
                    ? new CopyOnWriteArrayList<>()
                    : channelSessions;
            if (current.isEmpty()) {
                webSocketMetricsService.collaborationRoomOpened(resourceType(session));
            }
            current.add(session);
            return current;
        });
    }

    private void replayStoredUpdates(WebSocketSession session) throws IOException {
        JwtPrincipal principal = principal(session);
        for (byte[] payload : collabCrdtService.listUpdatePayloads(
                principal.userId(),
                resourceType(session),
                resourceId(session)
        )) {
            long started = System.nanoTime();
            session.sendMessage(new BinaryMessage(payload));
            recordMessage(DIRECTION_DOWN, TYPE_UPDATE, started);
        }
    }

    private byte[] readPayload(ByteBuffer buffer) {
        byte[] payload = new byte[buffer.remaining()];
        buffer.get(payload);
        return payload;
    }

    private void recordMessage(String direction, String type, long startedNanos) {
        webSocketMetricsService.message(MODULE_COLLAB, direction, type, elapsedMs(startedNanos));
    }

    private long elapsedMs(long startedNanos) {
        return Duration.ofNanos(System.nanoTime() - startedNanos).toMillis();
    }

    private String closeReason(CloseStatus status) {
        if (status.getCode() == THROTTLED.getCode()) {
            return REASON_THROTTLE;
        }
        return status.getCode() == CloseStatus.NORMAL.getCode() ? REASON_CLIENT : REASON_SERVER;
    }

    private String channel(WebSocketSession session) {
        return resourceType(session) + "/" + resourceId(session);
    }

    private String pongFrame(WebSocketSession session) {
        return """
                {"type":"%s","channel":"%s","seq":%d,"payload":{}}
                """.formatted(TYPE_PONG, channel(session), CONTROL_FRAME_SEQ);
    }

    private String resourceType(WebSocketSession session) {
        return (String) session.getAttributes().get(CollabWebSocketHandshakeInterceptor.ATTR_RESOURCE_TYPE);
    }

    private String resourceId(WebSocketSession session) {
        return (String) session.getAttributes().get(CollabWebSocketHandshakeInterceptor.ATTR_RESOURCE_ID);
    }

    private JwtPrincipal principal(WebSocketSession session) {
        Object principal = session.getAttributes().get(CollabWebSocketHandshakeInterceptor.ATTR_PRINCIPAL);
        if (principal instanceof JwtPrincipal jwtPrincipal) {
            return jwtPrincipal;
        }
        throw new IllegalStateException("collab websocket principal is required");
    }
}
