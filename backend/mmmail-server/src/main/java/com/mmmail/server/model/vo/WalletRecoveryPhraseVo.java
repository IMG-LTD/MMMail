package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record WalletRecoveryPhraseVo(
        String accountId,
        String recoveryPhrase,
        String recoveryFingerprint,
        int wordCount,
        LocalDateTime revealedAt
) {
}
