package com.mmmail.server.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public record QueryCalendarAvailabilityRequest(
        @NotNull LocalDateTime startAt,
        @NotNull LocalDateTime endAt,
        List<@NotBlank @Email @Size(max = 254) String> attendeeEmails,
        Long excludeEventId
) {
}
