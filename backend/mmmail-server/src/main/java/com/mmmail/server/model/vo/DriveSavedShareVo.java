package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record DriveSavedShareVo(
        String id,
        String shareId,
        String token,
        String itemId,
        String itemType,
        String itemName,
        String ownerEmail,
        String ownerDisplayName,
        String permission,
        String status,
        LocalDateTime expiresAt,
        LocalDateTime savedAt,
        boolean available,
        DriveShareReadableE2eeVo e2ee
) {
}
