package com.mmmail.server.service;

import com.mmmail.server.model.vo.AuditEventVo;
import com.mmmail.server.model.vo.SuiteNotificationSyncEventVo;
import com.mmmail.server.model.vo.SuiteNotificationSyncVo;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class SuiteNotificationSyncService {

    private static final int DEFAULT_SYNC_LIMIT = 12;
    private static final int MAX_SYNC_LIMIT = 50;
    private static final long STREAM_TIMEOUT_MS = 0L;
    private static final String KIND_BOOTSTRAP = "BOOTSTRAP";
    private static final String KIND_SYNC = "SYNC";
    private static final String KIND_UPDATE = "UPDATE";
    private static final String NOTIFICATION_EVENT_PREFIX = "SUITE_NOTIFICATION_";
    private static final Set<String> SYNC_EVENT_TYPES = Set.of(
            "SUITE_NOTIFICATION_MARK_READ",
            "SUITE_NOTIFICATION_MARK_ALL_READ",
            "SUITE_NOTIFICATION_ARCHIVE",
            "SUITE_NOTIFICATION_IGNORE",
            "SUITE_NOTIFICATION_RESTORE",
            "SUITE_NOTIFICATION_SNOOZE",
            "SUITE_NOTIFICATION_ASSIGN",
            "SUITE_NOTIFICATION_UNDO"
    );

    private final AuditService auditService;
    private final ConcurrentHashMap<Long, CopyOnWriteArrayList<SseEmitter>> emitterRegistry = new ConcurrentHashMap<>();

    public SuiteNotificationSyncService(AuditService auditService) {
        this.auditService = auditService;
    }

    public SseEmitter openStream(Long userId, Long afterEventId) {
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MS);
        registerEmitter(userId, emitter);
        sendPayload(emitter, buildSyncPayload(KIND_BOOTSTRAP, userId, afterEventId, DEFAULT_SYNC_LIMIT));
        return emitter;
    }

    public SuiteNotificationSyncVo getNotificationSync(Long userId, Long afterEventId, Integer limit) {
        return buildSyncPayload(KIND_SYNC, userId, afterEventId, normalizeSyncLimit(limit));
    }

    public long getCurrentCursor(Long userId) {
        AuditEventVo latestEvent = auditService.latestActorEvent(userId, SYNC_EVENT_TYPES);
        return latestEvent == null || latestEvent.id() == null ? 0L : latestEvent.id();
    }

    public String buildSyncVersion(Long cursor) {
        long safeCursor = cursor == null || cursor < 0 ? 0L : cursor;
        return "NTF-" + safeCursor;
    }

    public void publish(Long userId, AuditEventVo event) {
        if (userId == null || !isSyncEvent(event)) {
            return;
        }
        SuiteNotificationSyncVo payload = new SuiteNotificationSyncVo(
                KIND_UPDATE,
                LocalDateTime.now(),
                event.id(),
                buildSyncVersion(event.id()),
                true,
                1,
                List.of(toSyncEvent(event))
        );
        for (SseEmitter emitter : emitterRegistry.getOrDefault(userId, new CopyOnWriteArrayList<>())) {
            if (!sendPayload(emitter, payload)) {
                removeEmitter(userId, emitter);
            }
        }
    }

    private SuiteNotificationSyncVo buildSyncPayload(String kind, Long userId, Long afterEventId, int limit) {
        List<SuiteNotificationSyncEventVo> items = loadSyncEvents(userId, afterEventId, limit);
        long syncCursor = getCurrentCursor(userId);
        return new SuiteNotificationSyncVo(
                kind,
                LocalDateTime.now(),
                syncCursor,
                buildSyncVersion(syncCursor),
                !items.isEmpty(),
                items.size(),
                items
        );
    }

    private List<SuiteNotificationSyncEventVo> loadSyncEvents(Long userId, Long afterEventId, int limit) {
        boolean incremental = afterEventId != null && afterEventId > 0;
        List<AuditEventVo> events = auditService.listActorEvents(userId, SYNC_EVENT_TYPES, afterEventId, limit, incremental);
        if (!incremental) {
            Collections.reverse(events);
        }
        return events.stream().map(this::toSyncEvent).toList();
    }

    private SuiteNotificationSyncEventVo toSyncEvent(AuditEventVo event) {
        return new SuiteNotificationSyncEventVo(
                event.id(),
                event.eventType(),
                resolveOperation(event.eventType()),
                parseAuditDetailValue(event.detail(), "operationId"),
                parseAuditDetailInt(event.detail(), "requested", 0),
                parseAuditDetailInt(event.detail(), "affected", 0),
                parseAuditDetailValue(event.detail(), "sessionId"),
                event.createdAt()
        );
    }

    private String resolveOperation(String eventType) {
        if (!StringUtils.hasText(eventType)) {
            return "UNKNOWN";
        }
        if (eventType.startsWith(NOTIFICATION_EVENT_PREFIX)) {
            return eventType.substring(NOTIFICATION_EVENT_PREFIX.length());
        }
        return eventType;
    }

    private int normalizeSyncLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_SYNC_LIMIT;
        }
        return Math.max(1, Math.min(limit, MAX_SYNC_LIMIT));
    }

    private boolean isSyncEvent(AuditEventVo event) {
        return event != null && StringUtils.hasText(event.eventType()) && SYNC_EVENT_TYPES.contains(event.eventType());
    }

    private void registerEmitter(Long userId, SseEmitter emitter) {
        emitterRegistry.computeIfAbsent(userId, ignored -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> removeEmitter(userId, emitter));
        emitter.onTimeout(() -> removeEmitter(userId, emitter));
        emitter.onError(error -> removeEmitter(userId, emitter));
    }

    private void removeEmitter(Long userId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> emitters = emitterRegistry.get(userId);
        if (emitters == null) {
            return;
        }
        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            emitterRegistry.remove(userId);
        }
    }

    private boolean sendPayload(SseEmitter emitter, SuiteNotificationSyncVo payload) {
        try {
            emitter.send(SseEmitter.event().name("notification-sync").data(payload));
            return true;
        } catch (IOException exception) {
            emitter.completeWithError(exception);
            return false;
        }
    }

    private int parseAuditDetailInt(String detail, String key, int defaultValue) {
        String rawValue = parseAuditDetailValue(detail, key);
        if (!StringUtils.hasText(rawValue)) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(rawValue);
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }

    private String parseAuditDetailValue(String detail, String key) {
        if (!StringUtils.hasText(detail)) {
            return null;
        }
        for (String fragment : detail.split(",")) {
            String[] pair = fragment.trim().split("=", 2);
            if (pair.length != 2) {
                continue;
            }
            if (key.equals(pair[0].trim())) {
                return pair[1].trim();
            }
        }
        return null;
    }
}
