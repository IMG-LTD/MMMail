package com.mmmail.server.model.vo;

import java.util.List;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        UserProfileVo user,
        String risk,
        List<String> riskReasons,
        Boolean secondFactorRequired,
        String securityEventId,
        List<String> entitlements,
        List<String> featureFlags,
    String currentOrgId
) {
    public AuthResponse(String accessToken, String refreshToken, UserProfileVo user) {
        this(accessToken, refreshToken, user, "low", List.of(), false, null, List.of("community"), List.of("feat.community.enabled"), null);
    }
}
