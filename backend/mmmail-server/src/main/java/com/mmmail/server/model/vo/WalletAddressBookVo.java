package com.mmmail.server.model.vo;

import java.util.List;

public record WalletAddressBookVo(
        String accountId,
        String addressKind,
        String query,
        int total,
        int gapLimit,
        int consecutiveUnusedCount,
        List<WalletReceiveAddressVo> addresses
) {
}
