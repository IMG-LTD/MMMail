package com.mmmail.platform.access;

import com.mmmail.platform.contract.V21ApiContract;

public record AccessRequest(
        String method,
        String path,
        Long userId,
        String role,
        String orgId,
        String scopeId,
        V21ApiContract contract
) {

    public AccessRequest {
        method = requireText(method, "HTTP method");
        path = requireText(path, "Request path");
    }

    public boolean authenticated() {
        return userId != null;
    }

    public boolean admin() {
        return "ADMIN".equalsIgnoreCase(role);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
