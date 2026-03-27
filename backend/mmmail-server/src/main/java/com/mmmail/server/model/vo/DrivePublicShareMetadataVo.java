package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record DrivePublicShareMetadataVo(
        String shareId,
        String token,
        String itemId,
        String itemType,
        String itemName,
        String mimeType,
        long sizeBytes,
        String permission,
        String status,
        LocalDateTime expiresAt,
        boolean passwordProtected
) {
}
