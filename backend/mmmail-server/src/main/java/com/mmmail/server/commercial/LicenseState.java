package com.mmmail.server.commercial;

import java.time.Instant;

public record LicenseState(
        long orgId,
        String claimsJson,
        LicenseStatus status,
        Instant syncedAt,
        Instant expiresAt
) {

    public LicenseState {
        if (orgId <= 0) {
            throw new IllegalArgumentException("orgId must be positive");
        }
        if (claimsJson == null || claimsJson.isBlank()) {
            throw new IllegalArgumentException("claimsJson is required");
        }
        if (status == null || syncedAt == null || expiresAt == null) {
            throw new IllegalArgumentException("status, syncedAt and expiresAt are required");
        }
    }
}
