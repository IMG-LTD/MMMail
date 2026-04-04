package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record DriveShareLinkVo(
        String id,
        String itemId,
        String token,
        String permission,
        LocalDateTime expiresAt,
        String status,
        boolean passwordProtected,
        DriveShareReadableE2eeVo e2ee,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
