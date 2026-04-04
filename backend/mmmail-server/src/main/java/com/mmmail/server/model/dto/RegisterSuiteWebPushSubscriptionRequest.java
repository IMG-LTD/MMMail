package com.mmmail.server.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterSuiteWebPushSubscriptionRequest(
        @NotBlank
        @Size(max = 1024)
        String endpoint,
        @NotBlank
        @Size(max = 255)
        String p256dh,
        @NotBlank
        @Size(max = 255)
        String auth,
        @NotBlank
        @Size(max = 32)
        String contentEncoding,
        @Size(max = 255)
        String userAgent
) {
}
