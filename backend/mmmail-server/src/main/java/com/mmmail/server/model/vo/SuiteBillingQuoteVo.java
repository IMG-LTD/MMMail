package com.mmmail.server.model.vo;

public record SuiteBillingQuoteVo(
        String offerCode,
        String offerName,
        String quoteStatus,
        String checkoutMode,
        String currencyCode,
        String billingCycle,
        int seatCount,
        String marketingBadge,
        SuiteInvoiceSummaryVo invoiceSummary,
        SuiteEntitlementSummaryVo entitlementSummary,
        SuiteOnboardingSummaryVo onboardingSummary
) {
}
