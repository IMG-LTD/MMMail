package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record OrgTeamSpaceVo(
        String id,
        String orgId,
        String name,
        String slug,
        String description,
        String rootItemId,
        long storageBytes,
        long storageLimitBytes,
        long itemCount,
        String createdBy,
        String currentAccessRole,
        boolean canWrite,
        boolean canManage,
        LocalDateTime updatedAt
) {
}
