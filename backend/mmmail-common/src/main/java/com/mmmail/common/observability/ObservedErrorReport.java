package com.mmmail.common.observability;

import java.time.LocalDateTime;

public record ObservedErrorReport(
        String source,
        String category,
        String severity,
        String path,
        String method,
        Integer status,
        Integer errorCode,
        String message,
        String detail,
        String requestId,
        String userId,
        String sessionId,
        String orgId,
        String userAgent,
        LocalDateTime occurredAt
) {
}
