package com.mmmail.platform.access;

public enum AccessDecisionReason {
    ALLOWED,
    PUBLIC_CONTRACT,
    AUTHENTICATION_REQUIRED,
    ENTITLEMENT_REQUIRED,
    PERMISSION_DENIED,
    UNKNOWN_CONTRACT
}
