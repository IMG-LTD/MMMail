package com.mmmail.server.model.enums;

public enum VpnDefaultConnectionMode {
    FASTEST,
    RANDOM,
    LAST_CONNECTION,
    PROFILE;

    public static VpnDefaultConnectionMode resolve(String candidate, VpnDefaultConnectionMode fallback) {
        if (candidate == null || candidate.isBlank()) {
            return fallback == null ? FASTEST : fallback;
        }
        for (VpnDefaultConnectionMode value : values()) {
            if (value.name().equalsIgnoreCase(candidate.trim())) {
                return value;
            }
        }
        return fallback == null ? FASTEST : fallback;
    }
}
