package com.mmmail.server.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCalendarSubscriptionRequest(
        @NotBlank @Size(max = 2048) String url,
        @NotBlank @Size(max = 128) String label,
        @Size(max = 32) String authMode,
        @Size(max = 32) String color
) {
}
