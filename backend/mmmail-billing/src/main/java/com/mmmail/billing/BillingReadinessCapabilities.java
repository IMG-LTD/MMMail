package com.mmmail.billing;

import java.util.List;
import java.util.Map;

public record BillingReadinessCapabilities(List<String> panels, boolean legacyExitReady) {

    public static BillingReadinessCapabilities defaultCapabilities() {
        return new BillingReadinessCapabilities(List.of("plans", "billing", "operations", "boundary"), true);
    }

    public Map<String, Object> toPayload() {
        return Map.of(
                "panels", panels,
                "legacyExitReady", legacyExitReady
        );
    }
}
