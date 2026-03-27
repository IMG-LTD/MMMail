package com.mmmail.server.service;

import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.model.entity.WalletAccount;
import com.mmmail.server.model.entity.WalletAccountProfile;
import com.mmmail.server.model.entity.WalletEmailTransfer;
import com.mmmail.server.model.entity.WalletReceiveAddress;
import com.mmmail.server.model.vo.WalletAccountProfileVo;
import com.mmmail.server.model.vo.WalletAdvancedSettingsVo;
import com.mmmail.server.model.vo.WalletEmailTransferVo;
import com.mmmail.server.model.vo.WalletReceiveAddressVo;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class WalletParitySupport {

    public static final String ADDRESS_TYPE_NATIVE_SEGWIT = "NATIVE_SEGWIT";
    public static final String ADDRESS_TYPE_NESTED_SEGWIT = "NESTED_SEGWIT";
    public static final String ADDRESS_TYPE_LEGACY = "LEGACY";
    public static final String ADDRESS_TYPE_TAPROOT = "TAPROOT";
    public static final String ADDRESS_KIND_RECEIVE = "RECEIVE";
    public static final String ADDRESS_KIND_CHANGE = "CHANGE";
    public static final String ADDRESS_STATUS_UNUSED = "UNUSED";
    public static final String ADDRESS_STATUS_USED = "USED";
    public static final String ADDRESS_SOURCE_PRIMARY = "PRIMARY";
    public static final String ADDRESS_SOURCE_ROTATED = "ROTATED";
    public static final String ADDRESS_SOURCE_INTERNAL = "INTERNAL";
    public static final int DEFAULT_ACCOUNT_INDEX = 0;
    public static final int DEFAULT_ADDRESS_POOL_SIZE = 3;
    public static final int MAX_ACCOUNT_INDEX = 255;
    public static final int MAX_ADDRESS_POOL_SIZE = 12;
    public static final int IMPORT_WORD_COUNT_MIN = 12;
    public static final int IMPORT_WORD_COUNT_STEP = 3;
    public static final int IMPORT_WORD_COUNT_MAX = 24;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern SEED_WORD_PATTERN = Pattern.compile("^[A-Za-z]+$");
    private static final Set<String> SUPPORTED_ADDRESS_TYPES = Set.of(
            ADDRESS_TYPE_NATIVE_SEGWIT,
            ADDRESS_TYPE_NESTED_SEGWIT,
            ADDRESS_TYPE_LEGACY,
            ADDRESS_TYPE_TAPROOT
    );
    private static final Set<String> SUPPORTED_ADDRESS_KINDS = Set.of(ADDRESS_KIND_RECEIVE, ADDRESS_KIND_CHANGE);
    private static final String[] WORD_BANK = {
            "anchor", "amber", "breeze", "canyon", "cinder", "cobalt", "cosmos", "drift",
            "ember", "falcon", "frost", "glacier", "harbor", "iris", "juniper", "keystone",
            "lantern", "meadow", "nova", "onyx", "orbit", "petal", "quartz", "raven",
            "saffron", "solstice", "summit", "thunder", "topaz", "velvet", "willow", "zenith"
    };

    public WalletAdvancedSettingsVo toAdvancedSettingsVo(WalletAccount account) {
        return new WalletAdvancedSettingsVo(
                String.valueOf(account.getId()),
                account.getWalletName(),
                account.getAssetSymbol(),
                account.getAddress(),
                normalizeAddressType(account.getAddressType(), ADDRESS_TYPE_NATIVE_SEGWIT),
                normalizeAccountIndex(account.getAccountIndex(), DEFAULT_ACCOUNT_INDEX),
                account.getImported() != null && account.getImported() == 1,
                normalizeOptionalText(account.getWalletSourceFingerprint(), 64, "Wallet fingerprint is invalid"),
                account.getWalletPassphraseProtected() != null && account.getWalletPassphraseProtected() == 1,
                account.getImportedAt(),
                account.getUpdatedAt()
        );
    }

    public WalletAccountProfileVo toProfileVo(WalletAccountProfile profile) {
        return new WalletAccountProfileVo(
                String.valueOf(profile.getAccountId()),
                profile.getBitcoinViaEmailEnabled() != null && profile.getBitcoinViaEmailEnabled() == 1,
                profile.getAliasEmail(),
                profile.getBalanceMasked() != null && profile.getBalanceMasked() == 1,
                profile.getAddressPrivacyEnabled() != null && profile.getAddressPrivacyEnabled() == 1,
                profile.getAddressPoolSize() == null ? DEFAULT_ADDRESS_POOL_SIZE : profile.getAddressPoolSize(),
                profile.getRecoveryFingerprint(),
                previewPhrase(profile.getRecoveryPhrase()),
                profile.getPassphraseHint(),
                profile.getLastRecoveryViewedAt(),
                profile.getUpdatedAt()
        );
    }

    public WalletReceiveAddressVo toReceiveAddressVo(WalletReceiveAddress address, long valueMinor) {
        return new WalletReceiveAddressVo(
                String.valueOf(address.getId()),
                address.getAddress(),
                address.getLabel(),
                address.getSourceType(),
                normalizeAddressKind(address.getAddressKind()),
                address.getAddressIndex() == null ? 0 : address.getAddressIndex(),
                resolveAddressStatus(address.getAddressStatus(), valueMinor),
                valueMinor,
                address.getReservedFor(),
                address.getCreatedAt(),
                address.getUpdatedAt()
        );
    }

    public WalletEmailTransferVo toEmailTransferVo(WalletEmailTransfer transfer) {
        return new WalletEmailTransferVo(
                String.valueOf(transfer.getId()),
                String.valueOf(transfer.getTransactionId()),
                transfer.getRecipientEmail(),
                transfer.getDeliveryMessage(),
                transfer.getClaimCode(),
                transfer.getStatus(),
                transfer.getInviteRequired() != null && transfer.getInviteRequired() == 1,
                transfer.getAmountMinor() == null ? 0L : transfer.getAmountMinor(),
                transfer.getAssetSymbol(),
                transfer.getClaimedAt(),
                transfer.getCreatedAt(),
                transfer.getUpdatedAt()
        );
    }

    public String normalizeWalletName(String walletName, String fallback) {
        String candidate = StringUtils.hasText(walletName) ? walletName.trim() : fallback;
        if (!StringUtils.hasText(candidate) || candidate.length() < 2 || candidate.length() > 64) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Wallet name length is invalid");
        }
        return candidate;
    }

    public String normalizeAssetSymbol(String assetSymbol) {
        String candidate = normalizeOptionalText(assetSymbol, 16, "Wallet asset symbol is invalid");
        if (!StringUtils.hasText(candidate) || candidate.length() < 2) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Wallet asset symbol is invalid");
        }
        return candidate.toUpperCase(Locale.ROOT);
    }

    public String normalizeAddressType(String addressType, String fallback) {
        String candidate = StringUtils.hasText(addressType) ? addressType.trim().toUpperCase(Locale.ROOT) : fallback;
        if (!StringUtils.hasText(candidate)) {
            candidate = ADDRESS_TYPE_NATIVE_SEGWIT;
        }
        if (!SUPPORTED_ADDRESS_TYPES.contains(candidate)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Wallet address type is invalid");
        }
        return candidate;
    }

    public int normalizeAccountIndex(Integer accountIndex, Integer fallback) {
        int candidate = accountIndex != null ? accountIndex : (fallback == null ? DEFAULT_ACCOUNT_INDEX : fallback);
        if (candidate < DEFAULT_ACCOUNT_INDEX || candidate > MAX_ACCOUNT_INDEX) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Wallet account index is invalid");
        }
        return candidate;
    }

    public int normalizeAddressPoolSize(Integer requested, Integer fallback) {
        int candidate = requested == null ? (fallback == null ? DEFAULT_ADDRESS_POOL_SIZE : fallback) : requested;
        if (candidate < 1 || candidate > MAX_ADDRESS_POOL_SIZE) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Wallet address pool size is invalid");
        }
        return candidate;
    }

    public String normalizeLabel(String label, String fallback) {
        String candidate = StringUtils.hasText(label) ? label.trim() : fallback;
        if (candidate.length() < 2 || candidate.length() > 64) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Wallet receive address label length is invalid");
        }
        return candidate;
    }

    public String normalizeEmail(String email, String message) {
        String candidate = normalizeOptionalText(email, 254, message);
        if (!StringUtils.hasText(candidate) || !EMAIL_PATTERN.matcher(candidate).matches()) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, message);
        }
        return candidate.toLowerCase(Locale.ROOT);
    }

    public String normalizeOptionalText(String value, int maxLength, String message) {
        if (value == null) {
            return "";
        }
        String candidate = value.trim();
        if (candidate.length() > maxLength) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, message);
        }
        return candidate;
    }

    public String normalizeAddressKind(String addressKind) {
        String candidate = StringUtils.hasText(addressKind) ? addressKind.trim().toUpperCase(Locale.ROOT) : ADDRESS_KIND_RECEIVE;
        if (!SUPPORTED_ADDRESS_KINDS.contains(candidate)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Wallet address kind is invalid");
        }
        return candidate;
    }

    public String normalizeAddressQuery(String query) {
        if (!StringUtils.hasText(query)) {
            return "";
        }
        return query.trim().toLowerCase(Locale.ROOT);
    }

    public String normalizeSeedPhrase(String seedPhrase) {
        String normalized = normalizeOptionalText(seedPhrase, 512, "Wallet seed phrase is invalid")
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);
        if (!StringUtils.hasText(normalized)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Wallet seed phrase is invalid");
        }
        String[] words = normalized.split(" ");
        if (words.length < IMPORT_WORD_COUNT_MIN
                || words.length > IMPORT_WORD_COUNT_MAX
                || (words.length - IMPORT_WORD_COUNT_MIN) % IMPORT_WORD_COUNT_STEP != 0) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Wallet seed phrase must be BIP39-compatible");
        }
        for (String word : words) {
            if (!SEED_WORD_PATTERN.matcher(word).matches()) {
                throw new BizException(ErrorCode.INVALID_ARGUMENT, "Wallet seed phrase must be BIP39-compatible");
            }
        }
        return normalized;
    }

    public boolean matchesAddressQuery(WalletReceiveAddress address, String query) {
        if (!StringUtils.hasText(query)) {
            return true;
        }
        String candidate = query.toLowerCase(Locale.ROOT);
        return contains(address.getAddress(), candidate)
                || contains(address.getLabel(), candidate)
                || contains(address.getReservedFor(), candidate);
    }

    public String generateRecoveryPhrase() {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < IMPORT_WORD_COUNT_MIN; index += 1) {
            if (index > 0) {
                builder.append(' ');
            }
            builder.append(WORD_BANK[RANDOM.nextInt(WORD_BANK.length)]);
        }
        return builder.toString();
    }

    public String previewPhrase(String phrase) {
        if (!StringUtils.hasText(phrase)) {
            return "";
        }
        String[] words = phrase.trim().split("\\s+");
        if (words.length < 4) {
            return phrase.trim();
        }
        return String.join(" ", words[0], words[1], words[2], "•••");
    }

    public String fingerprintOf(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return toHex(bytes).substring(0, 16).toUpperCase(Locale.ROOT);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    public String generateClaimCode() {
        return "CLAIM-" + Long.toUnsignedString(RANDOM.nextLong(), 36).toUpperCase(Locale.ROOT);
    }

    public String generateAccountAddress(String addressType, int accountIndex, String sourceFingerprint) {
        String prefix = resolveAddressPrefix(addressType, false);
        String normalizedSeed = fingerprintOf(sourceFingerprint + "|acct|" + addressType + "|" + accountIndex).toLowerCase(Locale.ROOT);
        return prefix + normalizedSeed + Integer.toHexString(accountIndex).toLowerCase(Locale.ROOT);
    }

    public String generateDerivedAddress(
            String addressType,
            int accountIndex,
            String addressKind,
            int addressIndex,
            String sourceFingerprint
    ) {
        String normalizedKind = normalizeAddressKind(addressKind);
        String prefix = resolveAddressPrefix(addressType, ADDRESS_KIND_CHANGE.equals(normalizedKind));
        String normalizedSeed = fingerprintOf(sourceFingerprint + "|" + normalizedKind + "|" + accountIndex + "|" + addressIndex)
                .toLowerCase(Locale.ROOT);
        return prefix + normalizedSeed + Integer.toHexString(addressIndex).toLowerCase(Locale.ROOT);
    }

    public String resolveAddressStatus(String currentStatus, long valueMinor) {
        if (valueMinor > 0) {
            return ADDRESS_STATUS_USED;
        }
        if (StringUtils.hasText(currentStatus)) {
            return currentStatus.trim().toUpperCase(Locale.ROOT);
        }
        return ADDRESS_STATUS_UNUSED;
    }

    private boolean contains(String value, String query) {
        return StringUtils.hasText(value) && value.toLowerCase(Locale.ROOT).contains(query);
    }

    private String resolveAddressPrefix(String addressType, boolean internalFlow) {
        String normalizedType = normalizeAddressType(addressType, ADDRESS_TYPE_NATIVE_SEGWIT);
        if (ADDRESS_TYPE_LEGACY.equals(normalizedType)) {
            return "1";
        }
        if (ADDRESS_TYPE_NESTED_SEGWIT.equals(normalizedType)) {
            return "3";
        }
        if (ADDRESS_TYPE_TAPROOT.equals(normalizedType)) {
            return "bc1p";
        }
        return internalFlow ? "bc1qchg" : "bc1q";
    }

    private String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(Character.forDigit((value >> 4) & 0xF, 16));
            builder.append(Character.forDigit(value & 0xF, 16));
        }
        return builder.toString();
    }
}
