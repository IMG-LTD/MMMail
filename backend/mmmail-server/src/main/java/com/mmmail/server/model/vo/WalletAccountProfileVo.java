package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record WalletAccountProfileVo(
        String accountId,
        boolean bitcoinViaEmailEnabled,
        String aliasEmail,
        boolean balanceMasked,
        boolean addressPrivacyEnabled,
        int addressPoolSize,
        String recoveryFingerprint,
        String recoveryPhrasePreview,
        String passphraseHint,
        LocalDateTime lastRecoveryViewedAt,
        LocalDateTime updatedAt
) {
}
