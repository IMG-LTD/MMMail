package com.mmmail.server.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ImportCalendarIcsRequest(
        @NotBlank String content,
        @Size(max = 64) String timezone,
        @Min(0) @Max(10080) Integer reminderMinutes
) {
}
