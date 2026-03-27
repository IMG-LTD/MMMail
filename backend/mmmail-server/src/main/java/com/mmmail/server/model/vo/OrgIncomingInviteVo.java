package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record OrgIncomingInviteVo(
        String inviteId,
        String orgId,
        String orgName,
        String orgSlug,
        String role,
        String status,
        String invitedByEmail,
        LocalDateTime updatedAt
) {
}
