package com.mmmail.server.model.dto;

import jakarta.validation.constraints.Size;

public record RotateWalletReceiveAddressRequest(
        @Size(max = 64) String label
) {
}
