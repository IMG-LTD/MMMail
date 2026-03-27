package com.mmmail.server.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RemediateWalletTransactionRequest(
        @NotBlank @Size(min = 4, max = 32) String strategy,
        @Size(max = 256) String reason
) {
}
