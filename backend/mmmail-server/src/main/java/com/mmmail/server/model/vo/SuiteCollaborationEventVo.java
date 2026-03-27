package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record SuiteCollaborationEventVo(
        long eventId,
        String productCode,
        String eventType,
        String title,
        String summary,
        String routePath,
        String actorEmail,
        String sessionId,
        LocalDateTime createdAt
) {
}
