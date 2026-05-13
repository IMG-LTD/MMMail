package com.mmmail.server.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record PatchV21SecuritySettingsRequest(
        Boolean mfaEnabled,
        @Email @Size(max = 190) String recoveryEmail
) {
}
