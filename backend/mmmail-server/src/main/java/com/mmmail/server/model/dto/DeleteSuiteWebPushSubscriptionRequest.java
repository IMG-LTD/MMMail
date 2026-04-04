package com.mmmail.server.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeleteSuiteWebPushSubscriptionRequest(
        @NotBlank
        @Size(max = 1024)
        String endpoint
) {
}
