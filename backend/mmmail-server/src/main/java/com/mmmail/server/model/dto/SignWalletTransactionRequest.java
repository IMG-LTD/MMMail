package com.mmmail.server.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignWalletTransactionRequest(
        @NotBlank @Size(min = 2, max = 64) String signerHint
) {
}
