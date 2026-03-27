package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record WalletAdvancedSettingsVo(
        String accountId,
        String walletName,
        String assetSymbol,
        String address,
        String addressType,
        int accountIndex,
        boolean imported,
        String walletSourceFingerprint,
        boolean walletPassphraseProtected,
        LocalDateTime importedAt,
        LocalDateTime updatedAt
) {
}
