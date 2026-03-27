package com.mmmail.server.model.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public record CreateCalendarEventRequest(
        @NotBlank @Size(max = 128) String title,
        @Size(max = 2000) String description,
        @Size(max = 256) String location,
        @NotNull LocalDateTime startAt,
        @NotNull LocalDateTime endAt,
        Boolean allDay,
        @Size(max = 64) String timezone,
        @Min(0) @Max(10080) Integer reminderMinutes,
        List<@Valid CalendarAttendeeInput> attendees
) {
}
