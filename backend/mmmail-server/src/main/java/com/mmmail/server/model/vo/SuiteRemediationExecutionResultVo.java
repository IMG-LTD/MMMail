package com.mmmail.server.model.vo;

import java.time.LocalDateTime;
import java.util.Map;

public record SuiteRemediationExecutionResultVo(
        String actionCode,
        String productCode,
        String status,
        String message,
        LocalDateTime executedAt,
        Map<String, Object> details
) {
}
