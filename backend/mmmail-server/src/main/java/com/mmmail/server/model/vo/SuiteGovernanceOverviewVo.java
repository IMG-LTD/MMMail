package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record SuiteGovernanceOverviewVo(
        LocalDateTime generatedAt,
        long totalRequests,
        long pendingReviewCount,
        long pendingSecondReviewCount,
        long approvedPendingExecutionCount,
        long rejectedCount,
        long executedCount,
        long executedWithFailureCount,
        long rolledBackCount,
        long rollbackWithFailureCount,
        long slaBreachedCount
) {
}
