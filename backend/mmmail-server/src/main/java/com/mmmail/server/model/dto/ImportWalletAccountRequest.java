package com.mmmail.server.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ImportWalletAccountRequest(
        @NotBlank @Size(min = 2, max = 64) String walletName,
        @NotBlank @Size(min = 2, max = 16) String assetSymbol,
        @NotBlank @Size(min = 16, max = 512) String seedPhrase,
        @Size(max = 128) String passphrase,
        @Size(max = 32) String addressType,
        Integer accountIndex
) {
}
