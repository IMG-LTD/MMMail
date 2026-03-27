package com.mmmail.server.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank @Size(min = 2, max = 64) String displayName,
        @Size(max = 2000) String signature,
        @NotBlank @Size(max = 64) String timezone,
        @Pattern(regexp = "en|zh-CN|zh-TW") @Size(max = 16) String preferredLocale,
        @Pattern(regexp = "PROTON_ADDRESS|EXTERNAL_ACCOUNT") @Size(max = 32) String mailAddressMode,
        @NotNull @Min(5) @Max(300) Integer autoSaveSeconds,
        @NotNull @Min(0) @Max(60) Integer undoSendSeconds,
        @Min(1) @Max(200) Integer driveVersionRetentionCount,
        @Min(1) @Max(3650) Integer driveVersionRetentionDays
) {
}
