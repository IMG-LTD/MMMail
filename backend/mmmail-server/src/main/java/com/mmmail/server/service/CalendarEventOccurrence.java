package com.mmmail.server.service;

import java.time.LocalDateTime;

record CalendarEventOccurrence(
        LocalDateTime startAt,
        LocalDateTime endAt,
        long index
) {
}
