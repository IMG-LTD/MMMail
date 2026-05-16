package com.mmmail.server.model.vo;

import java.time.Instant;
import java.util.List;

public record CommercialLicenseStatusVo(
        String orgId,
        State state,
        String edition,
        List<String> features,
        String externalBillingStatus,
        Instant expiresAt,
        Instant syncedAt,
        String requiredAction,
        String message
) {

    public CommercialLicenseStatusVo {
        features = List.copyOf(features);
    }

    public enum State {
        MISSING,
        ACTIVE,
        EXPIRED,
        INVALID
    }
}
