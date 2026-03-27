package com.mmmail.server.model.vo;

import java.time.LocalDateTime;
import java.util.List;

public record SuiteReadinessReportVo(
        LocalDateTime generatedAt,
        int overallScore,
        String overallRiskLevel,
        int highRiskProductCount,
        int criticalRiskProductCount,
        List<SuiteReadinessItemVo> items
) {
}
