package com.mmmail.server.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ConfirmWalletTransactionRequest(
        @NotNull @Min(0) @Max(999999) Integer confirmations,
        @NotBlank @Size(min = 8, max = 128) String networkTxHash
) {
}
