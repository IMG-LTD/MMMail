package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record DocsNoteSyncEventVo(
        long eventId,
        String eventType,
        String noteId,
        String sessionId,
        String actorEmail,
        LocalDateTime createdAt
) {
}
