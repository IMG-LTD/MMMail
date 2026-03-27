package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record CalendarAvailabilitySlotVo(
        LocalDateTime startAt,
        LocalDateTime endAt,
        boolean allDay
) {
}
