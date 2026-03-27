package com.mmmail.server.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record UpdateAuthenticatorSecurityRequest(
        @NotNull Boolean syncEnabled,
        @NotNull Boolean encryptedBackupEnabled,
        @NotNull Boolean pinProtectionEnabled,
        @NotNull @Min(60) @Max(3600) Integer lockTimeoutSeconds,
        @Pattern(regexp = "\\d{4,12}") String pin
) {
}
