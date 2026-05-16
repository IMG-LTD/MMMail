package com.mmmail.server.model.vo;

import java.util.List;

public record AuthUserInfoVo(
        String id,
        String userId,
        String userName,
        List<String> roles,
        List<String> buttons,
        String email,
        String displayName,
        String role,
        String mailAddressMode,
        List<String> entitlements,
        List<String> featureFlags,
        String currentOrgId
) {
}
