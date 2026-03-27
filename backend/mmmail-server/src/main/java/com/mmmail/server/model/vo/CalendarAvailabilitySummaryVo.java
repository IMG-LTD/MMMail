package com.mmmail.server.model.vo;

public record CalendarAvailabilitySummaryVo(
        int attendeeCount,
        int busyCount,
        int freeCount,
        int unknownCount,
        boolean hasConflicts
) {
}
