package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record CalendarIncomingShareVo(
        String shareId,
        String eventId,
        String eventTitle,
        String ownerEmail,
        String permission,
        String responseStatus,
        LocalDateTime updatedAt
) {
}
