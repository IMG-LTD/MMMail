package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record SuiteCheckoutDraftVo(
        String offerCode,
        String offerName,
        String quoteStatus,
        String checkoutMode,
        String currencyCode,
        String billingCycle,
        int seatCount,
        String marketingBadge,
        String organizationName,
        String domainName,
        SuiteInvoiceSummaryVo invoiceSummary,
        SuiteEntitlementSummaryVo entitlementSummary,
        SuiteOnboardingSummaryVo onboardingSummary,
        LocalDateTime updatedAt
) {
}
