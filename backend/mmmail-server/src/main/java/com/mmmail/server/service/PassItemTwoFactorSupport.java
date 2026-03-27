package com.mmmail.server.service;

import com.mmmail.server.model.entity.PassVaultItem;
import com.mmmail.server.model.vo.PassItemTwoFactorVo;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Set;

@Component
public class PassItemTwoFactorSupport {

    private static final Set<String> TWO_FACTOR_ITEM_TYPES = Set.of(
            PassBusinessConstants.ITEM_TYPE_LOGIN,
            PassBusinessConstants.ITEM_TYPE_PASSWORD
    );

    private final AuthenticatorEntrySupport authenticatorEntrySupport;

    public PassItemTwoFactorSupport(AuthenticatorEntrySupport authenticatorEntrySupport) {
        this.authenticatorEntrySupport = authenticatorEntrySupport;
    }

    public boolean supports(PassVaultItem item) {
        return item != null && supportsItemType(item.getItemType());
    }

    public boolean supportsItemType(String itemType) {
        return TWO_FACTOR_ITEM_TYPES.contains(itemType);
    }

    public boolean hasTwoFactor(PassVaultItem item) {
        return item != null && StringUtils.hasText(item.getTwoFactorSecretCiphertext());
    }

    public AuthenticatorEntrySupport.NormalizedAuthenticatorEntry normalize(
            String issuer,
            String accountName,
            String secretCiphertext,
            String algorithm,
            Integer digits,
            Integer periodSeconds
    ) {
        return authenticatorEntrySupport.normalize(
                issuer,
                accountName,
                secretCiphertext,
                algorithm,
                digits,
                periodSeconds
        );
    }

    public void apply(
            PassVaultItem item,
            AuthenticatorEntrySupport.NormalizedAuthenticatorEntry normalized,
            LocalDateTime now
    ) {
        item.setTwoFactorIssuer(normalized.issuer());
        item.setTwoFactorAccountName(normalized.accountName());
        item.setTwoFactorSecretCiphertext(normalized.secretCiphertext());
        item.setTwoFactorAlgorithm(normalized.algorithm());
        item.setTwoFactorDigits(normalized.digits());
        item.setTwoFactorPeriodSeconds(normalized.periodSeconds());
        item.setUpdatedAt(now);
    }

    public void clear(PassVaultItem item, LocalDateTime now) {
        item.setTwoFactorIssuer(null);
        item.setTwoFactorAccountName(null);
        item.setTwoFactorSecretCiphertext(null);
        item.setTwoFactorAlgorithm(null);
        item.setTwoFactorDigits(null);
        item.setTwoFactorPeriodSeconds(null);
        item.setUpdatedAt(now);
    }

    public PassItemTwoFactorVo toVo(PassVaultItem item) {
        if (!hasTwoFactor(item)) {
            return new PassItemTwoFactorVo(false, null, null, null, null, null, null);
        }
        return new PassItemTwoFactorVo(
                true,
                item.getTwoFactorIssuer(),
                item.getTwoFactorAccountName(),
                authenticatorEntrySupport.normalizeAlgorithm(item.getTwoFactorAlgorithm(), null),
                authenticatorEntrySupport.normalizeDigits(item.getTwoFactorDigits(), null),
                authenticatorEntrySupport.normalizePeriodSeconds(item.getTwoFactorPeriodSeconds(), null),
                item.getUpdatedAt()
        );
    }
}
