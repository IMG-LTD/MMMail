package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record PassBusinessPolicyVo(
        String orgId,
        int minimumPasswordLength,
        int maximumPasswordLength,
        boolean requireUppercase,
        boolean requireDigits,
        boolean requireSymbols,
        boolean allowMemorablePasswords,
        boolean allowExternalSharing,
        boolean allowItemSharing,
        boolean allowSecureLinks,
        boolean allowMemberVaultCreation,
        boolean allowExport,
        boolean forceTwoFactor,
        boolean allowPasskeys,
        boolean allowAliases,
        LocalDateTime updatedAt
) {
}
