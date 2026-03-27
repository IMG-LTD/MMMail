package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record PassPublicSecureLinkVo(
        String itemId,
        String sharedVaultId,
        String sharedVaultName,
        String itemType,
        String title,
        String website,
        String username,
        String secretCiphertext,
        String note,
        int maxViews,
        int currentViews,
        Integer remainingViews,
        LocalDateTime expiresAt
) {
}
