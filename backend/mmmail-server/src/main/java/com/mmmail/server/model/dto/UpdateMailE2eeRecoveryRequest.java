package com.mmmail.server.model.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateMailE2eeRecoveryRequest(
        @NotNull Boolean enabled,
        @Size(max = 65535) String encryptedPrivateKeyArmored
) {
}
