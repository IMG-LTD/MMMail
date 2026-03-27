package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record SheetsIncomingShareVo(
        String shareId,
        String workbookId,
        String workbookTitle,
        String ownerEmail,
        String ownerDisplayName,
        String permission,
        String responseStatus,
        LocalDateTime updatedAt
) {
}
