package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record OrgTeamSpaceTrashItemVo(
        String id,
        String parentId,
        String itemType,
        String name,
        String mimeType,
        long sizeBytes,
        String ownerEmail,
        LocalDateTime trashedAt,
        LocalDateTime purgeAfterAt,
        LocalDateTime updatedAt
) {
}
