package com.mmmail.server.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BatchAdvanceWalletTransactionsRequest(
        @NotNull Long accountId,
        @Min(1) @Max(20) Integer maxItems,
        @Size(max = 64) String operatorHint
) {
}

