package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record NotificationRealtimePayloadVo(
        String eventType,
        String operation,
        String operationId,
        int requestedCount,
        int affectedCount,
        String sessionId,
        LocalDateTime createdAt
) {
}
