package com.mmmail.server.model.vo;

public record WalletActionResultVo(
        WalletTransactionVo transaction,
        String fromStatus,
        String toStatus,
        String operation,
        String message
) {
}
