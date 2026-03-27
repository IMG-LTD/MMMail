package com.mmmail.server.model.vo;

import java.util.List;

public record SuiteReadinessItemVo(
        String productCode,
        String productName,
        String category,
        boolean enabledByPlan,
        int score,
        String riskLevel,
        List<SuiteReadinessSignalVo> signals,
        List<String> blockers,
        List<SuiteRemediationActionVo> actions
) {
}
