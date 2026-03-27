package com.mmmail.server.model.enums;

public enum VpnConnectionSource {
    MANUAL,
    QUICK_CONNECT,
    PROFILE;

    public static VpnConnectionSource resolve(String candidate, VpnConnectionSource fallback) {
        if (candidate == null || candidate.isBlank()) {
            return fallback == null ? MANUAL : fallback;
        }
        for (VpnConnectionSource value : values()) {
            if (value.name().equalsIgnoreCase(candidate.trim())) {
                return value;
            }
        }
        return fallback == null ? MANUAL : fallback;
    }
}
