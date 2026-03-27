package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record PassSecureLinkDashboardVo(
        String id,
        String itemId,
        String sharedVaultId,
        String sharedVaultName,
        String itemTitle,
        String itemWebsite,
        String itemUsername,
        String publicUrl,
        int maxViews,
        int currentViews,
        LocalDateTime expiresAt,
        LocalDateTime revokedAt,
        LocalDateTime createdAt,
        boolean active,
        String status
) {
}
