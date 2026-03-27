package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record PassSecureLinkVo(
        String id,
        String itemId,
        String sharedVaultId,
        String token,
        String publicUrl,
        int maxViews,
        int currentViews,
        LocalDateTime expiresAt,
        LocalDateTime revokedAt,
        LocalDateTime createdAt,
        boolean active
) {
}
