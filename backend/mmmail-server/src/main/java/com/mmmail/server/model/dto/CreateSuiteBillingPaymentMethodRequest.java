package com.mmmail.server.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateSuiteBillingPaymentMethodRequest(
        @NotBlank @Pattern(regexp = "CARD|PAYPAL|BITCOIN|CASH") String methodType,
        @NotBlank @Size(max = 80) String displayLabel,
        @Size(max = 24) String brand,
        @Pattern(regexp = "\\d{4}") String lastFour,
        @Pattern(regexp = "\\d{4}-\\d{2}") String expiresAt,
        Boolean makeDefault
) {
}
