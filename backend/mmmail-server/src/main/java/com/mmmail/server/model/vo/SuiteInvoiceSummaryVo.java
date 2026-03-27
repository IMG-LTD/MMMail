package com.mmmail.server.model.vo;

import java.util.List;

public record SuiteInvoiceSummaryVo(
        String currencyCode,
        String billingCycle,
        int seatCount,
        int billingMonths,
        long subtotalCents,
        long discountCents,
        long totalCents,
        List<SuiteInvoiceLineVo> lineItems
) {
}
