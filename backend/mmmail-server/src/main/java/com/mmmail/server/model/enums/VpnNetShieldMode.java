package com.mmmail.server.model.enums;

public enum VpnNetShieldMode {
    OFF,
    BLOCK_MALWARE,
    BLOCK_MALWARE_ADS_TRACKERS;

    public static VpnNetShieldMode resolve(String candidate, VpnNetShieldMode fallback) {
        if (candidate == null || candidate.isBlank()) {
            return fallback == null ? OFF : fallback;
        }
        for (VpnNetShieldMode value : values()) {
            if (value.name().equalsIgnoreCase(candidate.trim())) {
                return value;
            }
        }
        return fallback == null ? OFF : fallback;
    }
}
