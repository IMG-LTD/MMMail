package com.mmmail.server.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record UpdatePassBusinessPolicyRequest(
        @Min(8) @Max(64) Integer minimumPasswordLength,
        @Min(8) @Max(64) Integer maximumPasswordLength,
        Boolean requireUppercase,
        Boolean requireDigits,
        Boolean requireSymbols,
        Boolean allowMemorablePasswords,
        Boolean allowExternalSharing,
        Boolean allowItemSharing,
        Boolean allowSecureLinks,
        Boolean allowMemberVaultCreation,
        Boolean allowExport,
        Boolean forceTwoFactor,
        Boolean allowPasskeys,
        Boolean allowAliases
) {
}
