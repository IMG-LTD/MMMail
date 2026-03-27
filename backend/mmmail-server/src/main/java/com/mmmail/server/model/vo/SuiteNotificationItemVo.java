package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record SuiteNotificationItemVo(
        String notificationId,
        String channel,
        String severity,
        String title,
        String message,
        String routePath,
        String actionCode,
        String productCode,
        LocalDateTime createdAt,
        boolean read,
        LocalDateTime readAt,
        String workflowStatus,
        LocalDateTime snoozedUntil,
        Long assignedToUserId,
        String assignedToDisplayName
) {
}
