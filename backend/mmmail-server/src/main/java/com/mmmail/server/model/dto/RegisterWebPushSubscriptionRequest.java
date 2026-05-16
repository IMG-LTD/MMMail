package com.mmmail.server.model.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterWebPushSubscriptionRequest(
        @NotBlank @Size(max = 1024) String endpoint,
        @NotNull @Valid Keys keys,
        @Size(max = 255) String ua,
        @Size(max = 64) String label
) {
    public RegisterSuiteWebPushSubscriptionRequest toSuiteRequest() {
        return new RegisterSuiteWebPushSubscriptionRequest(endpoint, keys.p256dh(), keys.auth(), "aes128gcm", ua);
    }

    public record Keys(
            @NotBlank @Size(max = 255) String p256dh,
            @NotBlank @Size(max = 255) String auth
    ) {
    }
}
