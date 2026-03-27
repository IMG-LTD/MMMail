package com.mmmail.server.model.vo;

import java.time.LocalDateTime;
import java.util.List;

public record WalletReconciliationOverviewVo(
        String accountId,
        LocalDateTime generatedAt,
        int integrityScore,
        String riskLevel,
        int mismatchCount,
        int blockedCount,
        int failedCount,
        List<String> recommendedActions
) {
}
