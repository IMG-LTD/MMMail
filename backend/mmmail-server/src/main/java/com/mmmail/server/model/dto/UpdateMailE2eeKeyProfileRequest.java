package com.mmmail.server.model.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record UpdateMailE2eeKeyProfileRequest(
        @NotNull Boolean enabled,
        @Size(max = 65535) String publicKeyArmored,
        @Size(max = 65535) String encryptedPrivateKeyArmored,
        @Size(max = 64) String fingerprint,
        @Size(max = 64) String algorithm,
        LocalDateTime keyCreatedAt
) {
}
