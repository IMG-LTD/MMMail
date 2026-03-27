package com.mmmail.server.model.vo;

import java.time.LocalDateTime;
import java.util.List;

public record CalendarAvailabilityVo(
        LocalDateTime startAt,
        LocalDateTime endAt,
        CalendarAvailabilitySummaryVo summary,
        List<CalendarParticipantAvailabilityVo> attendees
) {
}
