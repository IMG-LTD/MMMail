package com.mmmail.server.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdateWalletAdvancedSettingsRequest(
        @Size(min = 2, max = 64) String walletName,
        @Size(max = 32) String addressType,
        @Min(0) @Max(255) Integer accountIndex
) {
}
