package com.mmmail.server.model.vo;

import java.time.LocalDateTime;
import java.util.List;

public record SuiteSecurityPostureVo(
        LocalDateTime generatedAt,
        int securityScore,
        String overallRiskLevel,
        long activeSessionCount,
        long blockedSenderCount,
        long trustedSenderCount,
        long blockedDomainCount,
        long trustedDomainCount,
        int highRiskProductCount,
        int criticalRiskProductCount,
        List<String> alerts,
        List<SuiteRemediationActionVo> recommendedActions
) {
}
