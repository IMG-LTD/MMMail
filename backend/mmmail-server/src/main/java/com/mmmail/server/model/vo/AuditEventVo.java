package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record AuditEventVo(
        Long id,
        String eventType,
        String targetType,
        String targetId,
        String severity,
        String ipAddress,
        String detail,
        LocalDateTime createdAt
) {
}
