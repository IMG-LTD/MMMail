package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record OrgTeamSpaceMemberVo(
        String id,
        String orgId,
        String teamSpaceId,
        String userId,
        String userEmail,
        String role,
        boolean currentUser,
        LocalDateTime updatedAt
) {
}
