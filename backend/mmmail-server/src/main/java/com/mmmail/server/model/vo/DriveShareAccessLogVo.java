package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record DriveShareAccessLogVo(
        String id,
        String shareId,
        String itemId,
        String action,
        String accessStatus,
        String ipAddress,
        String userAgent,
        LocalDateTime createdAt
) {
}
