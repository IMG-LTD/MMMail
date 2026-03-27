package com.mmmail.server.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateAuthenticatorBackupRequest(
        @NotBlank @Size(min = 8, max = 128) String passphrase
) {
}
