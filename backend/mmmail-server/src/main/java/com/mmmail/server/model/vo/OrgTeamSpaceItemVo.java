package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record OrgTeamSpaceItemVo(
        String id,
        String teamSpaceId,
        String parentId,
        String itemType,
        String name,
        String mimeType,
        long sizeBytes,
        String ownerEmail,
        LocalDateTime updatedAt
) {
}
