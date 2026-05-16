package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record CommunityReportVo(
        String id,
        String targetType,
        String targetId,
        Long reporterUserId,
        String reason,
        String detail,
        String status,
        Long assigneeUserId,
        String action,
        String actionNote,
        LocalDateTime createdAt,
        LocalDateTime actionedAt
) {
}
