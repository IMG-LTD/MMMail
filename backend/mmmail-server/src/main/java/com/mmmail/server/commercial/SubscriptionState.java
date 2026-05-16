package com.mmmail.server.commercial;

import java.time.Instant;

public record SubscriptionState(
        long orgId,
        SubscriptionPlan plan,
        SubscriptionStatus status,
        BillingProviderType provider,
        Instant updatedAt
) {
}
