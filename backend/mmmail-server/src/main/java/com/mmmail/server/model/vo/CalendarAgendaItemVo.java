package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record CalendarAgendaItemVo(
        String id,
        String title,
        String location,
        LocalDateTime startAt,
        LocalDateTime endAt,
        int attendeeCount,
        boolean shared,
        String ownerEmail,
        String sharePermission
) {
}
