package com.mmmail.server.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;

public record BatchReconcileWalletTransactionsRequest(
        @NotNull Long accountId,
        @Min(1) @Max(20) Integer maxItems,
        @Size(min = 4, max = 32) String strategy
) {
}
