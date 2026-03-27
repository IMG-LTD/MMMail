package com.mmmail.server.model.vo;

public record WalletStageCountsVo(
        int pendingCount,
        int signedCount,
        int broadcastedCount,
        int confirmedCount,
        int failedCount
) {
}
