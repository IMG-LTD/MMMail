package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record OrgAuditEventVo(
        String id,
        String orgId,
        String actorId,
        String actorEmail,
        String eventType,
        String targetType,
        String targetId,
        String severity,
        String ipAddress,
        String detail,
        LocalDateTime createdAt
) {
}
