package com.mmmail.server.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateCalendarSettingsRequest(
        @NotBlank @Size(max = 64) String defaultTimezone,
        @NotBlank @Size(max = 32) String weekStartsOn,
        @NotEmpty List<@NotBlank @Size(max = 8) String> workingHours
) {
}
