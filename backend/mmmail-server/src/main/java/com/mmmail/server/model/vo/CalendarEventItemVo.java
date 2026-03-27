package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record CalendarEventItemVo(
        String id,
        String title,
        String location,
        LocalDateTime startAt,
        LocalDateTime endAt,
        boolean allDay,
        String timezone,
        Integer reminderMinutes,
        int attendeeCount,
        LocalDateTime updatedAt,
        boolean shared,
        String ownerEmail,
        String sharePermission,
        boolean canEdit,
        boolean canDelete
) {
}
