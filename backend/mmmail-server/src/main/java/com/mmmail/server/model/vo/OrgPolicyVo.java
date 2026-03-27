package com.mmmail.server.model.vo;

import java.time.LocalDateTime;
import java.util.List;

public record OrgPolicyVo(
        String orgId,
        List<String> allowedEmailDomains,
        int memberLimit,
        int governanceReviewSlaHours,
        boolean adminCanInviteAdmin,
        boolean adminCanRemoveAdmin,
        boolean adminCanReviewGovernance,
        boolean adminCanExecuteGovernance,
        boolean requireDualReviewGovernance,
        String twoFactorEnforcementLevel,
        int twoFactorGracePeriodDays,
        LocalDateTime updatedAt
) {
}
