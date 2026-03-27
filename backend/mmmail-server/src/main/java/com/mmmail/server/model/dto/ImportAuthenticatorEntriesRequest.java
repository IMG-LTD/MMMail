package com.mmmail.server.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ImportAuthenticatorEntriesRequest(
        @NotBlank @Size(max = 200000) String content,
        @Size(max = 32) String format
) {
}
