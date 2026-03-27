package com.mmmail.server.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.time.LocalDateTime;

public record CreatePassSecureLinkRequest(
        @Min(1) @Max(1000) Integer maxViews,
        LocalDateTime expiresAt
) {
}
