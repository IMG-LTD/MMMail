package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record PassItemSummaryVo(
        String id,
        String title,
        String website,
        String username,
        boolean favorite,
        LocalDateTime updatedAt,
        String scopeType,
        String itemType,
        String sharedVaultId,
        int secureLinkCount
) {
}
