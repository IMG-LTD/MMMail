package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record OrgCustomDomainVo(
        String id,
        String orgId,
        String domain,
        String verificationToken,
        String status,
        boolean defaultDomain,
        String createdBy,
        LocalDateTime verifiedAt,
        LocalDateTime updatedAt
) {
}
