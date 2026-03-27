package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record AuthenticatorSecurityPreferenceVo(
        boolean syncEnabled,
        boolean encryptedBackupEnabled,
        boolean pinProtectionEnabled,
        boolean pinConfigured,
        int lockTimeoutSeconds,
        LocalDateTime lastSyncedAt,
        LocalDateTime lastBackupAt
) {
}
