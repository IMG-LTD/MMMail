package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record SuiteBillingSubscriptionSummaryVo(
        String activePlanCode,
        String activePlanName,
        String billingCycle,
        int seatCount,
        boolean autoRenew,
        LocalDateTime currentPeriodEndsAt,
        Long defaultPaymentMethodId,
        String defaultPaymentMethodLabel,
        String pendingActionCode,
        String pendingOfferCode,
        String pendingOfferName,
        LocalDateTime pendingEffectiveAt
) {
}
