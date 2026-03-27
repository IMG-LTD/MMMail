package com.mmmail.server.service;

import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.model.entity.AuthenticatorEntry;
import com.mmmail.server.model.vo.AuthenticatorEntryDetailVo;
import com.mmmail.server.model.vo.AuthenticatorEntrySummaryVo;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

@Component
public class AuthenticatorEntrySupport {

    private static final String DEFAULT_ALGORITHM = "SHA1";
    private static final int DEFAULT_DIGITS = 6;
    private static final int DEFAULT_PERIOD_SECONDS = 30;
    private static final int MIN_DIGITS = 6;
    private static final int MAX_DIGITS = 8;
    private static final int MIN_PERIOD_SECONDS = 15;
    private static final int MAX_PERIOD_SECONDS = 120;
    private static final Set<String> SUPPORTED_ALGORITHMS = Set.of("SHA1", "SHA256", "SHA512");
    private static final int[] BASE32_LOOKUP = buildBase32Lookup();

    public NormalizedAuthenticatorEntry normalize(
            String issuer,
            String accountName,
            String secretCiphertext,
            String algorithm,
            Integer digits,
            Integer periodSeconds
    ) {
        return new NormalizedAuthenticatorEntry(
                requireIssuer(issuer),
                requireAccountName(accountName),
                requireSecret(secretCiphertext),
                normalizeAlgorithm(algorithm, null),
                normalizeDigits(digits, null),
                normalizePeriodSeconds(periodSeconds, null)
        );
    }

    public void apply(AuthenticatorEntry entry, NormalizedAuthenticatorEntry normalized, LocalDateTime now) {
        entry.setIssuer(normalized.issuer());
        entry.setAccountName(normalized.accountName());
        entry.setSecretCiphertext(normalized.secretCiphertext());
        entry.setAlgorithm(normalized.algorithm());
        entry.setDigits(normalized.digits());
        entry.setPeriodSeconds(normalized.periodSeconds());
        entry.setUpdatedAt(now);
    }

    public AuthenticatorEntry create(Long ownerId, NormalizedAuthenticatorEntry normalized, LocalDateTime now) {
        AuthenticatorEntry entry = new AuthenticatorEntry();
        entry.setOwnerId(ownerId);
        apply(entry, normalized, now);
        entry.setCreatedAt(now);
        entry.setDeleted(0);
        return entry;
    }

    public String normalizeAlgorithm(String algorithm, String fallback) {
        String candidate = StringUtils.hasText(algorithm) ? algorithm.trim().toUpperCase(Locale.ROOT) : fallback;
        if (!StringUtils.hasText(candidate)) {
            candidate = DEFAULT_ALGORITHM;
        }
        if (!SUPPORTED_ALGORITHMS.contains(candidate)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Unsupported authenticator algorithm");
        }
        return candidate;
    }

    public int normalizeDigits(Integer digits, Integer fallback) {
        int candidate = digits != null ? digits : (fallback != null ? fallback : DEFAULT_DIGITS);
        if (candidate < MIN_DIGITS || candidate > MAX_DIGITS) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Authenticator digits out of range");
        }
        return candidate;
    }

    public int normalizePeriodSeconds(Integer periodSeconds, Integer fallback) {
        int candidate = periodSeconds != null ? periodSeconds : (fallback != null ? fallback : DEFAULT_PERIOD_SECONDS);
        if (candidate < MIN_PERIOD_SECONDS || candidate > MAX_PERIOD_SECONDS) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Authenticator periodSeconds out of range");
        }
        return candidate;
    }

    public AuthenticatorEntrySummaryVo toSummaryVo(AuthenticatorEntry entry) {
        return new AuthenticatorEntrySummaryVo(
                String.valueOf(entry.getId()),
                entry.getIssuer(),
                entry.getAccountName(),
                normalizeAlgorithm(entry.getAlgorithm(), null),
                normalizeDigits(entry.getDigits(), null),
                normalizePeriodSeconds(entry.getPeriodSeconds(), null),
                entry.getUpdatedAt()
        );
    }

    public AuthenticatorEntryDetailVo toDetailVo(AuthenticatorEntry entry) {
        return new AuthenticatorEntryDetailVo(
                String.valueOf(entry.getId()),
                entry.getIssuer(),
                entry.getAccountName(),
                entry.getSecretCiphertext(),
                normalizeAlgorithm(entry.getAlgorithm(), null),
                normalizeDigits(entry.getDigits(), null),
                normalizePeriodSeconds(entry.getPeriodSeconds(), null),
                entry.getCreatedAt(),
                entry.getUpdatedAt()
        );
    }

    private String requireIssuer(String issuer) {
        if (!StringUtils.hasText(issuer)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Authenticator issuer is required");
        }
        return issuer.trim();
    }

    private String requireAccountName(String accountName) {
        if (!StringUtils.hasText(accountName)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Authenticator account is required");
        }
        return accountName.trim();
    }

    private String requireSecret(String secretCiphertext) {
        if (!StringUtils.hasText(secretCiphertext)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Authenticator secret is required");
        }
        String normalized = secretCiphertext.trim().replace(" ", "").replace("-", "").toUpperCase(Locale.ROOT);
        validateBase32(normalized);
        return normalized;
    }

    private void validateBase32(String secret) {
        if (!StringUtils.hasText(secret)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Authenticator secret is required");
        }
        for (int i = 0; i < secret.length(); i++) {
            char current = secret.charAt(i);
            if (current == '=') {
                continue;
            }
            if (current >= BASE32_LOOKUP.length || BASE32_LOOKUP[current] < 0) {
                throw new BizException(ErrorCode.INVALID_ARGUMENT, "Authenticator secret must be base32");
            }
        }
    }

    private static int[] buildBase32Lookup() {
        int[] lookup = new int[128];
        Arrays.fill(lookup, -1);
        for (int i = 0; i < 26; i++) {
            lookup['A' + i] = i;
        }
        for (int i = 0; i < 6; i++) {
            lookup['2' + i] = 26 + i;
        }
        return lookup;
    }

    public record NormalizedAuthenticatorEntry(
            String issuer,
            String accountName,
            String secretCiphertext,
            String algorithm,
            int digits,
            int periodSeconds
    ) {
    }
}
