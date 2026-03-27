package com.mmmail.server.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateWalletEmailTransferRequest(
        @Min(1) long amountMinor,
        @NotBlank @Size(max = 254) String recipientEmail,
        @Size(max = 256) String deliveryMessage,
        @Size(max = 512) String memo
) {
}
