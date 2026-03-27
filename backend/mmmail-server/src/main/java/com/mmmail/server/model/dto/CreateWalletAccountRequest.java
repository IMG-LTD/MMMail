package com.mmmail.server.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateWalletAccountRequest(
        @NotBlank @Size(min = 2, max = 64) String walletName,
        @NotBlank @Size(min = 2, max = 16) String assetSymbol,
        @NotBlank @Size(min = 16, max = 128) String address
) {
}
