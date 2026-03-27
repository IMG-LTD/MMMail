package com.mmmail.server.model.vo;

import java.util.List;

public record SuitePricingOfferVo(
        String code,
        String name,
        String description,
        String segment,
        String linkedPlanCode,
        String checkoutMode,
        String priceMode,
        String currencyCode,
        String priceValue,
        String originalPriceValue,
        String priceNote,
        String defaultBillingCycle,
        List<String> billingCycles,
        int defaultSeatCount,
        boolean seatEditable,
        boolean organizationRequired,
        boolean recommended,
        String marketingBadge,
        List<String> highlights,
        List<String> enabledProducts
) {
}
