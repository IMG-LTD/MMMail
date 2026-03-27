package com.mmmail.server.model.dto;

import jakarta.validation.constraints.Size;

public record AdvanceWalletTransactionRequest(
        @Size(max = 64) String operatorHint
) {
}
