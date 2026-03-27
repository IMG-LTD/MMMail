package com.mmmail.server.model.vo;

import java.util.List;

public record WalletPriorityTransactionVo(
        String transactionId,
        String status,
        int ageMinutes,
        String reason,
        List<String> recommendedActions
) {
}
