package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record PassIncomingSharedItemDetailVo(
        String shareId,
        String itemId,
        String title,
        String website,
        String username,
        String secretCiphertext,
        String note,
        String itemType,
        String sourceVaultId,
        String sourceVaultName,
        String ownerEmail,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean readOnly
) {
}
