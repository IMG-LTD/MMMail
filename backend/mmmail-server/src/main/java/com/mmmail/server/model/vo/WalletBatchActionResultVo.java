package com.mmmail.server.model.vo;

import java.util.List;

public record WalletBatchActionResultVo(
        String accountId,
        String operation,
        int requestedCount,
        int processedCount,
        int successCount,
        int failedCount,
        int skippedCount,
        List<WalletActionResultVo> results
) {
}

