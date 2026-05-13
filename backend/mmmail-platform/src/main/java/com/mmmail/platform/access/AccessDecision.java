package com.mmmail.platform.access;

import java.util.List;

public record AccessDecision(
        boolean allowed,
        AccessDecisionReason reason,
        int httpStatus,
        int errorCode,
        String message,
        AccessEntitlement requiredEntitlement,
        List<AccessPermission> requiredPermissions
) {

    private static final int HTTP_OK = 200;
    private static final int NO_ERROR_CODE = 0;

    public AccessDecision {
        if (reason == null) {
            throw new IllegalArgumentException("Access decision reason is required");
        }
        requiredPermissions = List.copyOf(requiredPermissions);
    }

    public static AccessDecision allowed(
            AccessDecisionReason reason,
            AccessEntitlement entitlement,
            List<AccessPermission> permissions
    ) {
        return new AccessDecision(true, reason, HTTP_OK, NO_ERROR_CODE, "Allowed", entitlement, permissions);
    }

    public static AccessDecision denied(
            AccessDecisionReason reason,
            int httpStatus,
            int errorCode,
            String message,
            AccessEntitlement entitlement,
            List<AccessPermission> permissions
    ) {
        return new AccessDecision(false, reason, httpStatus, errorCode, message, entitlement, permissions);
    }
}
