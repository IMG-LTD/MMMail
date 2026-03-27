package com.mmmail.server.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ImportAuthenticatorBackupRequest(
        @NotBlank @Size(max = 400000) String content,
        @NotBlank @Size(min = 8, max = 128) String passphrase
) {
}
