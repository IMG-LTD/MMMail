package com.mmmail.server.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdateWalletAccountProfileRequest(
        Boolean bitcoinViaEmailEnabled,
        @Size(max = 254) String aliasEmail,
        Boolean balanceMasked,
        Boolean addressPrivacyEnabled,
        @Min(1) @Max(12) Integer addressPoolSize,
        @Size(max = 128) String passphraseHint
) {
}
