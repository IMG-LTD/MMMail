package com.mmmail.server.model.vo;

public record SuiteBillingPaymentMethodVo(
        Long id,
        String methodType,
        String displayLabel,
        String brand,
        String lastFour,
        String expiresAt,
        boolean defaultMethod,
        String status
) {
}
