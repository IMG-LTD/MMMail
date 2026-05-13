package com.mmmail.platform.access;

import java.util.Set;

public record AccessPermission(String value) {

    private static final Set<String> PUBLIC_PERMISSIONS = Set.of(
            "auth:public",
            "share:public",
            "system:public"
    );

    public AccessPermission {
        value = normalize(value);
    }

    public boolean publicPermission() {
        return PUBLIC_PERMISSIONS.contains(value);
    }

    private static String normalize(String rawValue) {
        if (rawValue == null) {
            throw new IllegalArgumentException("Access permission is required");
        }
        String normalized = rawValue.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Access permission must not be blank");
        }
        if (!normalized.contains(":")) {
            throw new IllegalArgumentException("Access permission must contain namespace separator");
        }
        return normalized;
    }
}
