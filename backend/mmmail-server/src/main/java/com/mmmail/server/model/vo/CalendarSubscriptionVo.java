package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record CalendarSubscriptionVo(
        String id,
        String url,
        String label,
        String authMode,
        String color,
        String syncStatus,
        LocalDateTime lastSyncAt,
        String lastError,
        LocalDateTime nextSyncAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
