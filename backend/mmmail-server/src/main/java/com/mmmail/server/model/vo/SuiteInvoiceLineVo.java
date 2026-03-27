package com.mmmail.server.model.vo;

public record SuiteInvoiceLineVo(
        String lineCode,
        int quantity,
        long unitPriceCents,
        long totalPriceCents
) {
}
