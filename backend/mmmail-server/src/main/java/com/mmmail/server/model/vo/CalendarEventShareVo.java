package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record CalendarEventShareVo(
        String id,
        String eventId,
        String targetUserId,
        String targetEmail,
        String permission,
        String responseStatus,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
