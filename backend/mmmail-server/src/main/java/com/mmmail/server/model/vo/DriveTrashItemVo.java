package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record DriveTrashItemVo(
        String id,
        String parentId,
        String itemType,
        String name,
        String mimeType,
        long sizeBytes,
        LocalDateTime trashedAt,
        LocalDateTime purgeAfterAt,
        LocalDateTime updatedAt
) {
}
