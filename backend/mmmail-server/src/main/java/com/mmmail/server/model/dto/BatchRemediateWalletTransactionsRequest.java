package com.mmmail.server.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BatchRemediateWalletTransactionsRequest(
        @NotNull Long accountId,
        @Min(1) @Max(20) Integer maxItems,
        @NotBlank @Size(min = 4, max = 32) String strategy,
        @Size(max = 256) String reason
) {
}

