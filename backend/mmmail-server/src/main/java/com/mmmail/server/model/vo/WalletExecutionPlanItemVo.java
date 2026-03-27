package com.mmmail.server.model.vo;

public record WalletExecutionPlanItemVo(
        String transactionId,
        String status,
        String reason,
        String recommendedOperation,
        int priority
) {
}

