package com.mmmail.server.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmmail.server.model.vo.NotificationRealtimeEventVo;
import com.mmmail.server.model.vo.NotificationRealtimePayloadVo;
import com.mmmail.server.model.vo.NotificationRealtimeReplayVo;
import com.mmmail.server.model.vo.SuiteNotificationSyncEventVo;
import com.mmmail.server.model.vo.SuiteNotificationSyncVo;
import com.mmmail.server.observability.WebSocketMetricsService;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class NotificationRealtimeService {

    private static final String TYPE_BADGE_UPDATE = "badge-update";
    private static final String TYPE_NOTIFICATION = "notification";
    private static final String TYPE_PONG = "pong";
    private static final String CHANNEL_PREFIX = "user/u_";
    private static final String MODULE_NOTIFICATIONS = "notifications";
    private static final String DIRECTION_DOWN = "down";
    private static final long CONTROL_FRAME_SEQ = 0L;

    private final ObjectMapper objectMapper;
    private final WebSocketMetricsService webSocketMetricsService;
    private final ConcurrentHashMap<Long, CopyOnWriteArrayList<WebSocketSession>> sessions = new ConcurrentHashMap<>();

    public NotificationRealtimeService(ObjectMapper objectMapper, WebSocketMetricsService webSocketMetricsService) {
        this.objectMapper = objectMapper;
        this.webSocketMetricsService = webSocketMetricsService;
    }

    public void register(Long userId, WebSocketSession session) {
        sessions.computeIfAbsent(userId, ignored -> new CopyOnWriteArrayList<>()).add(session);
    }

    public void unregister(Long userId, WebSocketSession session) {
        CopyOnWriteArrayList<WebSocketSession> userSessions = sessions.get(userId);
        if (userSessions == null) {
            return;
        }
        userSessions.remove(session);
        if (userSessions.isEmpty()) {
            sessions.remove(userId);
        }
    }

    public void sendReplay(WebSocketSession session, Long userId, SuiteNotificationSyncVo payload) {
        for (NotificationRealtimeEventVo event : toReplay(userId, payload).events()) {
            send(session, event);
        }
    }

    public void publish(Long userId, SuiteNotificationSyncVo payload) {
        long started = System.nanoTime();
        List<WebSocketSession> userSessions = sessions.getOrDefault(userId, new CopyOnWriteArrayList<>());
        try {
            for (NotificationRealtimeEventVo event : toReplay(userId, payload).events()) {
                userSessions.forEach(session -> send(session, event));
            }
        } finally {
            webSocketMetricsService.notificationFanout(elapsedMs(started));
        }
    }

    public NotificationRealtimeReplayVo toReplay(Long userId, SuiteNotificationSyncVo payload) {
        List<NotificationRealtimeEventVo> events = payload.items().stream()
                .map(item -> toEvent(userId, item))
                .toList();
        long nextCursor = events.stream()
                .map(NotificationRealtimeEventVo::seq)
                .max(Comparator.naturalOrder())
                .orElse(payload.syncCursor());
        return new NotificationRealtimeReplayVo(events, nextCursor);
    }

    public void sendPong(WebSocketSession session, Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("notification websocket userId is required");
        }
        String frame = """
                {"type":"%s","channel":"%s%s","seq":%d,"payload":{}}
                """.formatted(TYPE_PONG, CHANNEL_PREFIX, userId, CONTROL_FRAME_SEQ);
        sendRaw(session, frame, TYPE_PONG);
    }

    private NotificationRealtimeEventVo toEvent(Long userId, SuiteNotificationSyncEventVo item) {
        if (item.eventId() == null) {
            throw new IllegalStateException("notification realtime event seq is required");
        }
        return new NotificationRealtimeEventVo(
                resolveType(item.operation()),
                CHANNEL_PREFIX + userId,
                item.eventId(),
                toPayload(item)
        );
    }

    private NotificationRealtimePayloadVo toPayload(SuiteNotificationSyncEventVo item) {
        return new NotificationRealtimePayloadVo(
                item.eventType(),
                item.operation(),
                item.operationId(),
                item.requestedCount(),
                item.affectedCount(),
                item.sessionId(),
                item.createdAt()
        );
    }

    private String resolveType(String operation) {
        return operation == null || operation.isBlank() ? TYPE_NOTIFICATION : TYPE_BADGE_UPDATE;
    }

    private void send(WebSocketSession session, NotificationRealtimeEventVo event) {
        try {
            sendRaw(session, objectMapper.writeValueAsString(event), event.type());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize notification realtime frame", exception);
        }
    }

    private void sendRaw(WebSocketSession session, String payload, String type) {
        long started = System.nanoTime();
        try {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(payload));
                webSocketMetricsService.message(MODULE_NOTIFICATIONS, DIRECTION_DOWN, type, elapsedMs(started));
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to send notification realtime frame", exception);
        }
    }

    private long elapsedMs(long startedNanos) {
        return Duration.ofNanos(System.nanoTime() - startedNanos).toMillis();
    }
}
