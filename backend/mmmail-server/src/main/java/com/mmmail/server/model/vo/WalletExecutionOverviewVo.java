package com.mmmail.server.model.vo;

import java.time.LocalDateTime;
import java.util.List;

public record WalletExecutionOverviewVo(
        String accountId,
        LocalDateTime generatedAt,
        int executionHealthScore,
        String riskLevel,
        WalletStageCountsVo stageCounts,
        int blockedCount,
        List<WalletPriorityTransactionVo> priorityTransactions
) {
}
