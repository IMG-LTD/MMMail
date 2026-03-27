package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record WalletEmailTransferVo(
        String transferId,
        String transactionId,
        String recipientEmail,
        String deliveryMessage,
        String claimCode,
        String status,
        boolean inviteRequired,
        long amountMinor,
        String assetSymbol,
        LocalDateTime claimedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
