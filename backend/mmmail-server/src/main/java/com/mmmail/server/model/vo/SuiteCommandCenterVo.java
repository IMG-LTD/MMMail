package com.mmmail.server.model.vo;

import java.time.LocalDateTime;
import java.util.List;

public record SuiteCommandCenterVo(
        LocalDateTime generatedAt,
        List<SuiteCommandItemVo> quickRoutes,
        List<SuiteCommandItemVo> pinnedSearches,
        List<String> recentKeywords,
        List<SuiteRemediationActionVo> recommendedActions,
        long pendingGovernanceCount,
        long securityAlertCount
) {
}
