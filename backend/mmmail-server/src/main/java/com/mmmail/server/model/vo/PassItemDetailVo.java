package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record PassItemDetailVo(
        String id,
        String title,
        String website,
        String username,
        String secretCiphertext,
        String note,
        boolean favorite,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String scopeType,
        String itemType,
        String orgId,
        String sharedVaultId,
        String ownerEmail,
        int secureLinkCount,
        PassItemTwoFactorVo twoFactor
) {
}
