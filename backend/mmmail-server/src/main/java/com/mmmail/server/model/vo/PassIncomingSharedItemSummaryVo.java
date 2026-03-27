package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record PassIncomingSharedItemSummaryVo(
        String shareId,
        String itemId,
        String title,
        String website,
        String username,
        String itemType,
        String sourceVaultId,
        String sourceVaultName,
        String ownerEmail,
        LocalDateTime updatedAt,
        boolean readOnly
) {
}
