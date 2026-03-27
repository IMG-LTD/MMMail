package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record PassMonitorItemVo(
        String id,
        String title,
        String website,
        String username,
        String itemType,
        String scopeType,
        String orgId,
        String sharedVaultId,
        String sharedVaultName,
        boolean excluded,
        boolean weakPassword,
        boolean reusedPassword,
        boolean inactiveTwoFactor,
        int reusedGroupSize,
        boolean canToggleExclusion,
        boolean canManageTwoFactor,
        PassItemTwoFactorVo twoFactor,
        LocalDateTime updatedAt
) {
}
