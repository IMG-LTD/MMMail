package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record PassBusinessOverviewVo(
        String orgId,
        String currentRole,
        int sharedVaultCount,
        int memberCount,
        int sharedItemCount,
        int secureLinkCount,
        int weakPasswordItemCount,
        int passkeyItemCount,
        int aliasItemCount,
        boolean allowSecureLinks,
        boolean allowExternalSharing,
        boolean forceTwoFactor,
        boolean allowPasskeys,
        boolean allowAliases,
        LocalDateTime lastActivityAt,
        LocalDateTime policyUpdatedAt
) {
}
