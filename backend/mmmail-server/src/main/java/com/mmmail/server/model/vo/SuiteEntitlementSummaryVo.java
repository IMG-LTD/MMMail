package com.mmmail.server.model.vo;

import java.util.List;

public record SuiteEntitlementSummaryVo(
        String offerCode,
        String linkedPlanCode,
        String primaryProductCode,
        String supportTier,
        String workspaceMode,
        int seatCount,
        boolean prioritySupport,
        List<String> unlockedProducts,
        List<String> highlights
) {
}
