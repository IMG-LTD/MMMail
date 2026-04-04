package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record SuiteWebPushSubscriptionVo(
        Long subscriptionId,
        String endpointHash,
        LocalDateTime lastSuccessAt,
        LocalDateTime lastFailureAt,
        String lastErrorMessage,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
