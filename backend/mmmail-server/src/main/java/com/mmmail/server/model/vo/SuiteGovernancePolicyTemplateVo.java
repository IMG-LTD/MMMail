package com.mmmail.server.model.vo;

import java.util.List;

public record SuiteGovernancePolicyTemplateVo(
        String templateCode,
        String name,
        String riskLevel,
        String description,
        List<String> actionCodes,
        List<String> rollbackActionCodes,
        boolean approvalRequired
) {
}
