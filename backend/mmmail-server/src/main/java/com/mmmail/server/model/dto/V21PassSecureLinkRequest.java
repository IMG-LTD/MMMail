package com.mmmail.server.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record V21PassSecureLinkRequest(
        @NotNull Long orgId,
        @NotNull Long itemId,
        @Min(1) @Max(1000) Integer maxViews,
        LocalDateTime expiresAt
) {
    public CreatePassSecureLinkRequest toCreateRequest() {
        return new CreatePassSecureLinkRequest(maxViews, expiresAt);
    }
}
