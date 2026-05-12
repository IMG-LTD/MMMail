package com.mmmail.platform.jobs;

import java.time.LocalDateTime;

public record JobRunMetadata(
        String tenantId,
        String userId,
        String requestId,
        String traceId,
        String module,
        String operation,
        LocalDateTime requestedAt
) {

    public JobRunMetadata {
        if (!hasText(module)) {
            throw new IllegalArgumentException("module is required");
        }
        if (!hasText(operation)) {
            throw new IllegalArgumentException("operation is required");
        }
        if (requestedAt == null) {
            throw new IllegalArgumentException("requestedAt is required");
        }
    }

    public void validateFor(JobRunType type) {
        if (type.tenantRequired() && !hasText(tenantId)) {
            throw new IllegalArgumentException("tenantId is required for " + type.jobName());
        }
        if (type.userRequired() && !hasText(userId)) {
            throw new IllegalArgumentException("userId is required for " + type.jobName());
        }
        if (!type.ownerModule().equals(module)) {
            throw new IllegalArgumentException("module must match job owner " + type.ownerModule());
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
