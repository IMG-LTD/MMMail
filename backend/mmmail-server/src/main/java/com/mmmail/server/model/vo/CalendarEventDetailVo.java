package com.mmmail.server.model.vo;

import java.time.LocalDateTime;
import java.util.List;

public record CalendarEventDetailVo(
        String id,
        String title,
        String description,
        String location,
        LocalDateTime startAt,
        LocalDateTime endAt,
        boolean allDay,
        String timezone,
        Integer reminderMinutes,
        List<CalendarAttendeeVo> attendees,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean shared,
        String ownerEmail,
        String sharePermission,
        boolean canEdit,
        boolean canDelete
) {
}
