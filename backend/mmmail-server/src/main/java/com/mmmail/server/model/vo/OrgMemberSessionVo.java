package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record OrgMemberSessionVo(
        String sessionId,
        String memberId,
        String userId,
        String memberEmail,
        String role,
        LocalDateTime createdAt,
        LocalDateTime expiresAt,
        boolean current
) {
}
