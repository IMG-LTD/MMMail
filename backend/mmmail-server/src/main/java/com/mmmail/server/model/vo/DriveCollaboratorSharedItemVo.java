package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record DriveCollaboratorSharedItemVo(
        String shareId,
        String itemId,
        String itemType,
        String itemName,
        String ownerEmail,
        String ownerDisplayName,
        String permission,
        String status,
        LocalDateTime updatedAt,
        boolean available
) {
}
