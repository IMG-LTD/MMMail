package com.mmmail.server.model.vo;

import java.time.LocalDateTime;
import java.util.List;

public record WalletExecutionPlanVo(
        String accountId,
        LocalDateTime generatedAt,
        int recommendedAdvanceCount,
        int recommendedRemediationCount,
        int estimatedRiskDelta,
        List<WalletExecutionPlanItemVo> items
) {
}

