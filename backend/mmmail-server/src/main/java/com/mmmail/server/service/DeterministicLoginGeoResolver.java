package com.mmmail.server.service;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class DeterministicLoginGeoResolver implements LoginGeoResolver {

    private static final String SOURCE = "DETERMINISTIC_V212_GEO";

    @Override
    public LoginGeoPoint resolve(String ipAddress) {
        String normalized = StringUtils.hasText(ipAddress) ? ipAddress.trim() : "0.0.0.0";
        if (normalized.startsWith("203.0.113.")) {
            return new LoginGeoPoint(normalized, "Brussels", "BE", 50.8503, 4.3517, SOURCE);
        }
        if (normalized.startsWith("198.51.100.")) {
            return new LoginGeoPoint(normalized, "Amsterdam", "NL", 52.3676, 4.9041, SOURCE);
        }
        if (normalized.startsWith("192.0.2.")) {
            return new LoginGeoPoint(normalized, "Tokyo", "JP", 35.6762, 139.6503, SOURCE);
        }
        if (isLocalAddress(normalized)) {
            return new LoginGeoPoint(normalized, "Local", "PRIVATE", 0.0, 0.0, SOURCE);
        }
        return new LoginGeoPoint(normalized, "Unknown", "UNRESOLVED", 0.0, 0.0, "UNRESOLVED");
    }

    private boolean isLocalAddress(String ipAddress) {
        return ipAddress.startsWith("127.")
                || ipAddress.startsWith("10.")
                || ipAddress.startsWith("172.16.")
                || ipAddress.startsWith("192.168.")
                || "0:0:0:0:0:0:0:1".equals(ipAddress)
                || "::1".equals(ipAddress);
    }
}
