package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record SuiteSubscriptionVo(
        String planCode,
        String planName,
        String status,
        LocalDateTime updatedAt,
        SuiteUsageVo usage,
        SuitePlanVo plan
) {
}
