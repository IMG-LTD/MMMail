package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record SuiteNotificationSyncEventVo(
        Long eventId,
        String eventType,
        String operation,
        String operationId,
        int requestedCount,
        int affectedCount,
        String sessionId,
        LocalDateTime createdAt
) {
}
