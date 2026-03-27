package com.mmmail.server.model.vo;

import java.util.List;

public record SuiteOnboardingSummaryVo(
        String onboardingMode,
        String nextAction,
        boolean organizationRequired,
        List<String> checklistCodes
) {
}
