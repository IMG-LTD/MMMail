package com.mmmail.server.commercial;

import java.time.Clock;
import java.time.Instant;
import java.util.Set;

public record LicenseClaims(
        Long orgId,
        Edition edition,
        int seats,
        Set<FeatureCode> features,
        Instant issuedAt,
        Instant expiresAt
) {

    public LicenseClaims {
        if (orgId == null || orgId <= 0) {
            throw new IllegalArgumentException("orgId must be positive");
        }
        if (edition == null) {
            throw new IllegalArgumentException("edition is required");
        }
        if (seats <= 0) {
            throw new IllegalArgumentException("seats must be positive");
        }
        if (features == null) {
            throw new IllegalArgumentException("features is required");
        }
        if (issuedAt == null || expiresAt == null) {
            throw new IllegalArgumentException("issuedAt and expiresAt are required");
        }
        if (!expiresAt.isAfter(issuedAt)) {
            throw new IllegalArgumentException("expiresAt must be after issuedAt");
        }
        features = Set.copyOf(features);
    }

    public boolean isExpired(Clock clock) {
        return !expiresAt.isAfter(clock.instant());
    }
}
