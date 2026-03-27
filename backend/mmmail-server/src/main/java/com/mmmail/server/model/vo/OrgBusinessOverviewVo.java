package com.mmmail.server.model.vo;

import java.time.LocalDateTime;
import java.util.List;

public record OrgBusinessOverviewVo(
        String orgId,
        String orgName,
        String currentRole,
        int memberCount,
        int adminCount,
        int pendingInviteCount,
        int teamSpaceCount,
        long storageBytes,
        long storageLimitBytes,
        List<String> allowedEmailDomains,
        int governanceReviewSlaHours,
        boolean requireDualReviewGovernance,
        LocalDateTime generatedAt
) {
}
