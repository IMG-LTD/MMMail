package com.mmmail.server.model.enums;

public enum MailAddressMode {
    PROTON_ADDRESS,
    EXTERNAL_ACCOUNT;

    public static boolean isExternalAccount(String value) {
        return EXTERNAL_ACCOUNT.name().equals(normalize(value));
    }

    public static String resolveStoredValue(String candidate, String fallback) {
        String resolvedCandidate = normalize(candidate);
        if (resolvedCandidate != null) {
            return resolvedCandidate;
        }
        String resolvedFallback = normalize(fallback);
        if (resolvedFallback != null) {
            return resolvedFallback;
        }
        return PROTON_ADDRESS.name();
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (MailAddressMode mode : values()) {
            if (mode.name().equals(value)) {
                return mode.name();
            }
        }
        return null;
    }
}
