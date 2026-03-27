package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record WalletTransactionVo(
        String transactionId,
        String accountId,
        String txType,
        String counterpartyAddress,
        long amountMinor,
        String assetSymbol,
        String memo,
        String status,
        int confirmations,
        String signatureHash,
        String networkTxHash,
        LocalDateTime createdAt
) {
}
