package com.mmmail.server.model.enums;

public enum VpnProfileRoutingMode {
    FASTEST,
    COUNTRY,
    SERVER;

    public static VpnProfileRoutingMode resolve(String candidate, VpnProfileRoutingMode fallback) {
        if (candidate == null || candidate.isBlank()) {
            return fallback == null ? FASTEST : fallback;
        }
        for (VpnProfileRoutingMode value : values()) {
            if (value.name().equalsIgnoreCase(candidate.trim())) {
                return value;
            }
        }
        return fallback == null ? FASTEST : fallback;
    }
}
