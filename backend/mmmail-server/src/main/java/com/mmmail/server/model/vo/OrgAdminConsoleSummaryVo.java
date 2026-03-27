package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record OrgAdminConsoleSummaryVo(
        String orgId,
        String orgName,
        String currentRole,
        int memberCount,
        int adminCount,
        int domainCount,
        int verifiedDomainCount,
        int mailIdentityCount,
        int enabledMailIdentityCount,
        int enabledProductCount,
        String defaultDomain,
        String defaultSenderAddress,
        LocalDateTime generatedAt
) {
}
