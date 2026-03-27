package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record OrgWorkspaceVo(
        String id,
        String name,
        String slug,
        String role,
        String status,
        int memberCount,
        LocalDateTime updatedAt
) {
}
