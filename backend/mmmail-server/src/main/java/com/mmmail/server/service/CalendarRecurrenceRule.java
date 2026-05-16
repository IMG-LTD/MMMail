package com.mmmail.server.service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.Set;

record CalendarRecurrenceRule(
        String normalized,
        String frequency,
        int interval,
        LocalDateTime until,
        Integer count,
        Set<DayOfWeek> byDays
) {
}
