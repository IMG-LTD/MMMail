package com.mmmail.server.commercial;

import java.time.Instant;

public record BillingWebhookEvent(
        String eventId,
        long orgId,
        SubscriptionPlan plan,
        SubscriptionStatus status,
        Instant occurredAt,
        String signatureVersion
) {

    public BillingWebhookEvent {
        requireText(eventId, "eventId");
        requireText(signatureVersion, "signatureVersion");
        if (orgId <= 0) {
            throw new IllegalArgumentException("orgId must be positive");
        }
        if (plan == null || status == null || occurredAt == null) {
            throw new IllegalArgumentException("plan, status, and occurredAt are required");
        }
    }

    public String canonicalPayload() {
        return String.join("\n",
                signatureVersion,
                eventId,
                String.valueOf(orgId),
                plan.name(),
                status.name(),
                occurredAt.toString());
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
    }
}
