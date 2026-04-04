package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record DriveItemVo(
        String id,
        String parentId,
        String itemType,
        String name,
        String mimeType,
        long sizeBytes,
        int shareCount,
        DriveFileE2eeVo e2ee,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
