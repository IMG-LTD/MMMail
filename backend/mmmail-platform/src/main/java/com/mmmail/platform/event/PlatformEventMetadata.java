package com.mmmail.platform.event;

import java.time.LocalDateTime;

public record PlatformEventMetadata(
        String tenantId,
        String userId,
        String requestId,
        String traceId,
        String module,
        String operation,
        LocalDateTime occurredAt
) {

    public PlatformEventMetadata {
        if (!hasText(module)) {
            throw new IllegalArgumentException("module is required");
        }
        if (!hasText(operation)) {
            throw new IllegalArgumentException("operation is required");
        }
        if (occurredAt == null) {
            throw new IllegalArgumentException("occurredAt is required");
        }
    }

    public void validateFor(PlatformEventType type) {
        if (type.tenantRequired() && !hasText(tenantId)) {
            throw new IllegalArgumentException("tenantId is required for " + type.eventName());
        }
        if (type.userRequired() && !hasText(userId)) {
            throw new IllegalArgumentException("userId is required for " + type.eventName());
        }
        if (!type.ownerModule().equals(module)) {
            throw new IllegalArgumentException("module must match event owner " + type.ownerModule());
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
