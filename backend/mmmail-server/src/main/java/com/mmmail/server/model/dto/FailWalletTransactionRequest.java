package com.mmmail.server.model.dto;

import jakarta.validation.constraints.Size;

public record FailWalletTransactionRequest(
        @Size(max = 256) String reason
) {
}
