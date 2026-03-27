package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record SuiteBillingInvoiceVo(
        String invoiceNumber,
        String offerCode,
        String offerName,
        String invoiceStatus,
        String currencyCode,
        long totalCents,
        String billingCycle,
        int seatCount,
        LocalDateTime issuedAt,
        LocalDateTime dueAt,
        String downloadCode
) {
}
