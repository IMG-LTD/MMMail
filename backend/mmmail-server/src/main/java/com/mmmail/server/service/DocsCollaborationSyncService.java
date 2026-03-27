package com.mmmail.server.service;

import com.mmmail.server.model.vo.AuditEventVo;
import com.mmmail.server.model.vo.DocsNoteSyncEventVo;
import com.mmmail.server.model.vo.DocsNoteSyncVo;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class DocsCollaborationSyncService {

    private static final int DEFAULT_SYNC_LIMIT = 20;
    private static final int MAX_SYNC_LIMIT = 50;
    private static final long STREAM_TIMEOUT_MS = 0L;
    private static final String KIND_BOOTSTRAP = "BOOTSTRAP";
    private static final String KIND_SYNC = "SYNC";
    private static final String KIND_UPDATE = "UPDATE";
    private static final Set<String> SYNC_EVENT_TYPES = Set.of(
            "DOCS_NOTE_UPDATE",
            "DOCS_NOTE_SHARE_ADD",
            "DOCS_NOTE_SHARE_PERMISSION_UPDATE",
            "DOCS_NOTE_SHARE_REMOVE",
            "DOCS_NOTE_COMMENT_ADD",
            "DOCS_NOTE_COMMENT_RESOLVE",
            "DOCS_NOTE_SUGGEST_ADD",
            "DOCS_NOTE_SUGGEST_ACCEPT",
            "DOCS_NOTE_SUGGEST_REJECT"
    );

    private final AuditService auditService;
    private final ConcurrentHashMap<Long, CopyOnWriteArrayList<SseEmitter>> emitterRegistry = new ConcurrentHashMap<>();

    public DocsCollaborationSyncService(AuditService auditService) {
        this.auditService = auditService;
    }

    public SseEmitter openStream(Long noteId, Long afterEventId) {
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MS);
        registerEmitter(noteId, emitter);
        sendPayload(emitter, buildSyncPayload(KIND_BOOTSTRAP, noteId, afterEventId, DEFAULT_SYNC_LIMIT));
        return emitter;
    }

    public DocsNoteSyncVo getSync(Long noteId, Long afterEventId, Integer limit) {
        return buildSyncPayload(KIND_SYNC, noteId, afterEventId, normalizeSyncLimit(limit));
    }

    public long getCurrentCursor(Long noteId) {
        AuditEventVo latest = auditService.latestEventByDetail(SYNC_EVENT_TYPES, noteToken(noteId));
        return latest == null || latest.id() == null ? 0L : latest.id();
    }

    public String buildSyncVersion(Long cursor) {
        long safeCursor = cursor == null || cursor < 0 ? 0L : cursor;
        return "DOC-" + safeCursor;
    }

    public void publish(Long noteId, AuditEventVo event) {
        if (noteId == null || !isSyncEvent(event)) {
            return;
        }
        DocsNoteSyncVo payload = new DocsNoteSyncVo(
                KIND_UPDATE,
                LocalDateTime.now(),
                event.id(),
                buildSyncVersion(event.id()),
                true,
                1,
                List.of(toSyncEvent(event))
        );
        for (SseEmitter emitter : emitterRegistry.getOrDefault(noteId, new CopyOnWriteArrayList<>())) {
            if (!sendPayload(emitter, payload)) {
                removeEmitter(noteId, emitter);
            }
        }
    }

    private DocsNoteSyncVo buildSyncPayload(String kind, Long noteId, Long afterEventId, int limit) {
        List<DocsNoteSyncEventVo> items = loadSyncEvents(noteId, afterEventId, limit);
        long syncCursor = getCurrentCursor(noteId);
        return new DocsNoteSyncVo(
                kind,
                LocalDateTime.now(),
                syncCursor,
                buildSyncVersion(syncCursor),
                !items.isEmpty(),
                items.size(),
                items
        );
    }

    private List<DocsNoteSyncEventVo> loadSyncEvents(Long noteId, Long afterEventId, int limit) {
        boolean incremental = afterEventId != null && afterEventId > 0;
        List<AuditEventVo> events = auditService.listEventsByDetail(
                SYNC_EVENT_TYPES,
                noteToken(noteId),
                afterEventId,
                limit,
                incremental
        );
        if (!incremental) {
            java.util.Collections.reverse(events);
        }
        return events.stream().map(this::toSyncEvent).toList();
    }

    private DocsNoteSyncEventVo toSyncEvent(AuditEventVo event) {
        return new DocsNoteSyncEventVo(
                event.id(),
                event.eventType(),
                parseDetailValue(event.detail(), "noteId"),
                parseDetailValue(event.detail(), "sessionId"),
                parseDetailValue(event.detail(), "actorEmail"),
                event.createdAt()
        );
    }

    private String noteToken(Long noteId) {
        return "noteId=" + noteId + ";";
    }

    private String parseDetailValue(String detail, String key) {
        if (detail == null || key == null) {
            return "";
        }
        String prefix = key + "=";
        for (String part : detail.split(";")) {
            String trimmed = part.trim();
            if (trimmed.startsWith(prefix)) {
                return trimmed.substring(prefix.length());
            }
        }
        return "";
    }

    private boolean sendPayload(SseEmitter emitter, DocsNoteSyncVo payload) {
        try {
            emitter.send(SseEmitter.event().name("docs-sync").data(payload));
            return true;
        } catch (IOException exception) {
            emitter.completeWithError(exception);
            return false;
        }
    }

    private void registerEmitter(Long noteId, SseEmitter emitter) {
        emitterRegistry.computeIfAbsent(noteId, ignored -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> removeEmitter(noteId, emitter));
        emitter.onTimeout(() -> removeEmitter(noteId, emitter));
        emitter.onError(ignored -> removeEmitter(noteId, emitter));
    }

    private void removeEmitter(Long noteId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> emitters = emitterRegistry.get(noteId);
        if (emitters == null) {
            return;
        }
        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            emitterRegistry.remove(noteId);
        }
    }

    private int normalizeSyncLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_SYNC_LIMIT;
        }
        return Math.max(1, Math.min(limit, MAX_SYNC_LIMIT));
    }

    private boolean isSyncEvent(AuditEventVo event) {
        return event != null && event.eventType() != null && SYNC_EVENT_TYPES.contains(event.eventType());
    }
}
