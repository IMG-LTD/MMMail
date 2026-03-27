package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record OrgMailIdentityVo(
        String id,
        String orgId,
        String memberId,
        String memberEmail,
        String customDomainId,
        String localPart,
        String emailAddress,
        String displayName,
        String status,
        boolean defaultIdentity,
        String createdBy,
        LocalDateTime updatedAt
) {
}
