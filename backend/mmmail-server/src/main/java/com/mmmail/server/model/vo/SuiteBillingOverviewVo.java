package com.mmmail.server.model.vo;

import java.util.List;

public record SuiteBillingOverviewVo(
        String activePlanCode,
        String activePlanName,
        SuiteCheckoutDraftVo latestDraft,
        List<String> selfServeOfferCodes,
        List<String> contactSalesOfferCodes
) {
}
