package com.mmmail.server.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SendWalletTransactionRequest(
        @NotNull Long accountId,
        @Min(1) long amountMinor,
        @NotBlank @Size(min = 2, max = 16) String assetSymbol,
        @NotBlank @Size(min = 16, max = 128) String targetAddress,
        @Size(max = 512) String memo
) {
}
