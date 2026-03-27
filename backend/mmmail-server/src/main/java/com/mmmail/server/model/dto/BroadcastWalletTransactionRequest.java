package com.mmmail.server.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BroadcastWalletTransactionRequest(
        @NotBlank @Size(min = 8, max = 128) String networkTxHash
) {
}
