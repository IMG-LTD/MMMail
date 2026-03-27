package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record SuiteBillingSubscriptionActionVo(
        String actionCode,
        String actionStatus,
        boolean enabled,
        String targetOfferCode,
        String targetOfferName,
        LocalDateTime effectiveAt,
        String reasonCode
) {
}
