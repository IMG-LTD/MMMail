package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record WalletAccountVo(
        String accountId,
        String walletName,
        String assetSymbol,
        String address,
        long balanceMinor,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
