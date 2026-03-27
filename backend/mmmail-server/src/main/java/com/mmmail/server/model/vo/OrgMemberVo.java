package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record OrgMemberVo(
        String id,
        String orgId,
        String userId,
        String userEmail,
        String role,
        String status,
        String invitedBy,
        LocalDateTime joinedAt,
        LocalDateTime updatedAt
) {
}
