package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.mapper.UserAccountMapper;
import com.mmmail.server.mapper.WalletAccountMapper;
import com.mmmail.server.mapper.WalletAccountProfileMapper;
import com.mmmail.server.mapper.WalletEmailTransferMapper;
import com.mmmail.server.model.dto.ImportWalletAccountRequest;
import com.mmmail.server.model.dto.UpdateWalletAdvancedSettingsRequest;
import com.mmmail.server.model.entity.UserAccount;
import com.mmmail.server.model.entity.WalletAccount;
import com.mmmail.server.model.entity.WalletAccountProfile;
import com.mmmail.server.model.entity.WalletEmailTransfer;
import com.mmmail.server.model.vo.WalletAccountProfileVo;
import com.mmmail.server.model.vo.WalletAddressBookVo;
import com.mmmail.server.model.vo.WalletAdvancedSettingsVo;
import com.mmmail.server.model.vo.WalletEmailTransferVo;
import com.mmmail.server.model.vo.WalletParityWorkspaceVo;
import com.mmmail.server.model.vo.WalletReceiveAddressVo;
import com.mmmail.server.model.vo.WalletRecoveryPhraseVo;
import com.mmmail.server.model.vo.WalletTransactionVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class WalletParityService {

    private static final String WALLET_ASSET_BTC = "BTC";
    private static final String TRANSFER_PENDING = "PENDING_CLAIM";
    private static final String TRANSFER_CLAIMED = "CLAIMED";
    private static final int MAX_TRANSFER_LIMIT = 20;
    private static final Set<String> TRANSFER_STATUSES = Set.of(TRANSFER_PENDING, TRANSFER_CLAIMED);

    private final WalletAccountMapper walletAccountMapper;
    private final WalletAccountProfileMapper walletAccountProfileMapper;
    private final WalletEmailTransferMapper walletEmailTransferMapper;
    private final UserAccountMapper userAccountMapper;
    private final WalletService walletService;
    private final AuditService auditService;
    private final WalletParitySupport walletParitySupport;
    private final WalletParityAccountSupport walletParityAccountSupport;

    public WalletParityService(
            WalletAccountMapper walletAccountMapper,
            WalletAccountProfileMapper walletAccountProfileMapper,
            WalletEmailTransferMapper walletEmailTransferMapper,
            UserAccountMapper userAccountMapper,
            WalletService walletService,
            AuditService auditService,
            WalletParitySupport walletParitySupport,
            WalletParityAccountSupport walletParityAccountSupport
    ) {
        this.walletAccountMapper = walletAccountMapper;
        this.walletAccountProfileMapper = walletAccountProfileMapper;
        this.walletEmailTransferMapper = walletEmailTransferMapper;
        this.userAccountMapper = userAccountMapper;
        this.walletService = walletService;
        this.auditService = auditService;
        this.walletParitySupport = walletParitySupport;
        this.walletParityAccountSupport = walletParityAccountSupport;
    }
    @Transactional
    public WalletParityWorkspaceVo getWorkspace(Long userId, Long accountId, Integer limit, String ipAddress) {
        WalletAccount account = loadAccount(userId, accountId);
        WalletAccountProfile profile = ensureProfile(userId, account);
        walletParityAccountSupport.ensureAddressInventory(account);
        List<WalletReceiveAddressVo> receiveAddresses = walletParityAccountSupport.listAddressVos(
                account,
                WalletParitySupport.ADDRESS_KIND_RECEIVE
        );
        List<WalletReceiveAddressVo> changeAddresses = walletParityAccountSupport.listAddressVos(
                account,
                WalletParitySupport.ADDRESS_KIND_CHANGE
        );
        List<WalletEmailTransferVo> transfers = listTransferVos(account.getId(), userId, normalizeLimit(limit));
        auditService.record(userId, "WALLET_PARITY_WORKSPACE_QUERY", "accountId=" + accountId, ipAddress);
        return new WalletParityWorkspaceVo(
                walletParitySupport.toAdvancedSettingsVo(account),
                walletParitySupport.toProfileVo(profile),
                receiveAddresses,
                changeAddresses,
                transfers
        );
    }
    @Transactional
    public WalletAddressBookVo getAddressBook(Long userId, Long accountId, String kind, String query, String ipAddress) {
        WalletAccount account = loadAccount(userId, accountId);
        WalletAccountProfile profile = ensureProfile(userId, account);
        walletParityAccountSupport.ensureAddressInventory(account);
        String safeKind = walletParitySupport.normalizeAddressKind(kind);
        String safeQuery = walletParitySupport.normalizeAddressQuery(query);
        List<WalletReceiveAddressVo> allAddresses = walletParityAccountSupport.listAddressVos(account, safeKind);
        List<WalletReceiveAddressVo> filtered = walletParityAccountSupport.filterAddresses(allAddresses, safeQuery);
        auditService.record(userId, "WALLET_ADDRESS_BOOK_QUERY", "accountId=" + accountId + ",kind=" + safeKind, ipAddress);
        return new WalletAddressBookVo(
                String.valueOf(accountId),
                safeKind,
                safeQuery,
                filtered.size(),
                walletParitySupport.normalizeAddressPoolSize(profile.getAddressPoolSize(), WalletParitySupport.DEFAULT_ADDRESS_POOL_SIZE),
                walletParityAccountSupport.countTrailingUnused(allAddresses),
                filtered
        );
    }
    @Transactional
    public WalletAccountProfileVo updateProfile(
            Long userId,
            Long accountId,
            Boolean bitcoinViaEmailEnabled,
            String aliasEmail,
            Boolean balanceMasked,
            Boolean addressPrivacyEnabled,
            Integer addressPoolSize,
            String passphraseHint,
            String ipAddress
    ) {
        WalletAccount account = loadAccount(userId, accountId);
        WalletAccountProfile profile = ensureProfile(userId, account);
        applyProfileUpdates(
                profile,
                account,
                userId,
                bitcoinViaEmailEnabled,
                aliasEmail,
                balanceMasked,
                addressPrivacyEnabled,
                addressPoolSize,
                passphraseHint
        );
        profile.setUpdatedAt(LocalDateTime.now());
        walletAccountProfileMapper.updateById(profile);
        auditService.record(userId, "WALLET_PARITY_PROFILE_UPDATE", "accountId=" + accountId, ipAddress);
        return walletParitySupport.toProfileVo(profile);
    }
    @Transactional
    public WalletAdvancedSettingsVo updateAdvancedSettings(
            Long userId,
            Long accountId,
            UpdateWalletAdvancedSettingsRequest request,
            String ipAddress
    ) {
        WalletAccount account = loadAccount(userId, accountId);
        String safeWalletName = walletParitySupport.normalizeWalletName(request.walletName(), account.getWalletName());
        String safeAddressType = walletParitySupport.normalizeAddressType(request.addressType(), account.getAddressType());
        int safeAccountIndex = walletParitySupport.normalizeAccountIndex(request.accountIndex(), account.getAccountIndex());
        walletParityAccountSupport.ensureAccountSlotAvailable(
                userId,
                account.getId(),
                account.getAssetSymbol(),
                safeAddressType,
                safeAccountIndex
        );
        walletParityAccountSupport.applyAdvancedSettings(account, safeWalletName, safeAddressType, safeAccountIndex);
        walletAccountMapper.updateById(account);
        walletParityAccountSupport.ensureAddressInventory(account);
        auditService.record(userId, "WALLET_ADVANCED_SETTINGS_UPDATE", "accountId=" + accountId, ipAddress);
        return walletParitySupport.toAdvancedSettingsVo(account);
    }
    @Transactional
    public WalletAdvancedSettingsVo importWallet(Long userId, ImportWalletAccountRequest request, String ipAddress) {
        String safeWalletName = walletParitySupport.normalizeWalletName(request.walletName(), null);
        String safeAssetSymbol = requireImportAsset(request.assetSymbol());
        String safeSeedPhrase = walletParitySupport.normalizeSeedPhrase(request.seedPhrase());
        String safePassphrase = walletParitySupport.normalizeOptionalText(
                request.passphrase(),
                128,
                "Wallet import passphrase is too long"
        );
        String safeAddressType = walletParitySupport.normalizeAddressType(
                request.addressType(),
                WalletParitySupport.ADDRESS_TYPE_NATIVE_SEGWIT
        );
        int safeAccountIndex = walletParitySupport.normalizeAccountIndex(
                request.accountIndex(),
                WalletParitySupport.DEFAULT_ACCOUNT_INDEX
        );
        String sourceFingerprint = walletParitySupport.fingerprintOf(
                safeSeedPhrase + "|" + safePassphrase + "|" + safeAssetSymbol
        );
        walletParityAccountSupport.ensureAccountSlotAvailable(
                userId,
                null,
                safeAssetSymbol,
                safeAddressType,
                safeAccountIndex
        );
        WalletAccount account = walletParityAccountSupport.createImportedAccount(
                userId,
                safeWalletName,
                safeAssetSymbol,
                safeAddressType,
                safeAccountIndex,
                sourceFingerprint,
                StringUtils.hasText(safePassphrase)
        );
        walletAccountMapper.insert(account);
        walletParityAccountSupport.createImportedProfile(userId, account, sourceFingerprint, defaultAliasEmail(userId));
        walletParityAccountSupport.ensureAddressInventory(account);
        auditService.record(userId, "WALLET_ACCOUNT_IMPORT", "accountId=" + account.getId(), ipAddress);
        return walletParitySupport.toAdvancedSettingsVo(account);
    }
    @Transactional
    public WalletReceiveAddressVo rotateReceiveAddress(Long userId, Long accountId, String label, String ipAddress) {
        WalletAccount account = loadAccount(userId, accountId);
        WalletAccountProfile profile = ensureProfile(userId, account);
        requireAddressPrivacyEnabled(profile);
        WalletReceiveAddressVo address = walletParityAccountSupport.createRotatedReceiveAddress(
                account,
                walletParitySupport.normalizeLabel(label, "Receive")
        );
        auditService.record(
                userId,
                "WALLET_RECEIVE_ADDRESS_ROTATE",
                "accountId=" + accountId + ",addressId=" + address.addressId(),
                ipAddress
        );
        return address;
    }
    @Transactional
    public WalletEmailTransferVo createEmailTransfer(
            Long userId,
            Long accountId,
            long amountMinor,
            String recipientEmail,
            String deliveryMessage,
            String memo,
            String ipAddress
    ) {
        WalletAccount account = loadAccount(userId, accountId);
        WalletAccountProfile profile = ensureProfile(userId, account);
        requireEmailEnabled(profile);
        String safeRecipientEmail = walletParitySupport.normalizeEmail(
                recipientEmail,
                "Wallet recipient email is invalid"
        );
        String safeMessage = walletParitySupport.normalizeOptionalText(
                deliveryMessage,
                256,
                "Wallet delivery message is too long"
        );
        WalletTransactionVo tx = walletService.send(
                userId,
                accountId,
                amountMinor,
                account.getAssetSymbol(),
                "mailto:" + safeRecipientEmail,
                memo,
                ipAddress
        );
        WalletEmailTransfer transfer = createTransfer(account, tx, safeRecipientEmail, safeMessage);
        auditService.record(
                userId,
                "WALLET_EMAIL_TRANSFER_CREATE",
                "accountId=" + accountId + ",transferId=" + transfer.getId(),
                ipAddress
        );
        return walletParitySupport.toEmailTransferVo(transfer);
    }
    @Transactional
    public WalletEmailTransferVo claimEmailTransfer(Long userId, Long transferId, String ipAddress) {
        WalletEmailTransfer transfer = loadTransfer(userId, transferId);
        if (TRANSFER_CLAIMED.equals(transfer.getStatus())) {
            return walletParitySupport.toEmailTransferVo(transfer);
        }
        transfer.setStatus(TRANSFER_CLAIMED);
        transfer.setClaimedAt(LocalDateTime.now());
        transfer.setUpdatedAt(LocalDateTime.now());
        walletEmailTransferMapper.updateById(transfer);
        walletService.advanceTransaction(userId, transfer.getTransactionId(), "wallet-email-claim", ipAddress);
        auditService.record(userId, "WALLET_EMAIL_TRANSFER_CLAIM", "transferId=" + transferId, ipAddress);
        return walletParitySupport.toEmailTransferVo(transfer);
    }

    @Transactional
    public WalletRecoveryPhraseVo revealRecoveryPhrase(Long userId, Long accountId, String ipAddress) {
        WalletAccount account = loadAccount(userId, accountId);
        if (account.getImported() != null && account.getImported() == 1) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Imported wallet recovery phrase is not stored");
        }
        WalletAccountProfile profile = ensureProfile(userId, account);
        LocalDateTime now = LocalDateTime.now();
        profile.setLastRecoveryViewedAt(now);
        profile.setUpdatedAt(now);
        walletAccountProfileMapper.updateById(profile);
        auditService.record(userId, "WALLET_RECOVERY_REVEAL", "accountId=" + accountId, ipAddress);
        return new WalletRecoveryPhraseVo(
                String.valueOf(accountId),
                profile.getRecoveryPhrase(),
                profile.getRecoveryFingerprint(),
                profile.getRecoveryPhrase().trim().split("\\s+").length,
                now
        );
    }
    private void applyProfileUpdates(
            WalletAccountProfile profile,
            WalletAccount account,
            Long userId,
            Boolean bitcoinViaEmailEnabled,
            String aliasEmail,
            Boolean balanceMasked,
            Boolean addressPrivacyEnabled,
            Integer addressPoolSize,
            String passphraseHint
    ) {
        int emailEnabled = resolveFlag(bitcoinViaEmailEnabled, profile.getBitcoinViaEmailEnabled());
        String resolvedAlias = resolveAliasEmail(profile, userId, aliasEmail, emailEnabled == 1);
        if (emailEnabled == 1) {
            requireBitcoinAccount(account);
            ensureBitcoinViaEmailAvailable(profile.getAccountId(), userId, resolvedAlias);
        }
        profile.setBitcoinViaEmailEnabled(emailEnabled);
        profile.setAliasEmail(resolvedAlias);
        profile.setBalanceMasked(resolveFlag(balanceMasked, profile.getBalanceMasked()));
        profile.setAddressPrivacyEnabled(resolveFlag(addressPrivacyEnabled, profile.getAddressPrivacyEnabled()));
        profile.setAddressPoolSize(walletParitySupport.normalizeAddressPoolSize(addressPoolSize, profile.getAddressPoolSize()));
        profile.setPassphraseHint(walletParitySupport.normalizeOptionalText(
                passphraseHint,
                128,
                "Wallet passphrase hint is too long"
        ));
    }
    private String resolveAliasEmail(
            WalletAccountProfile profile,
            Long userId,
            String aliasEmail,
            boolean emailEnabled
    ) {
        if (StringUtils.hasText(aliasEmail)) {
            return walletParitySupport.normalizeEmail(aliasEmail, "Wallet alias email is invalid");
        }
        if (emailEnabled) {
            return defaultAliasEmail(userId);
        }
        return walletParitySupport.normalizeOptionalText(profile.getAliasEmail(), 254, "Wallet alias email is invalid");
    }
    private WalletAccountProfile ensureProfile(Long userId, WalletAccount account) {
        WalletAccountProfile existing = walletAccountProfileMapper.selectOne(new LambdaQueryWrapper<WalletAccountProfile>()
                .eq(WalletAccountProfile::getOwnerId, userId)
                .eq(WalletAccountProfile::getAccountId, account.getId()));
        if (existing != null) {
            return existing;
        }
        LocalDateTime now = LocalDateTime.now();
        WalletAccountProfile created = new WalletAccountProfile();
        created.setAccountId(account.getId());
        created.setOwnerId(userId);
        created.setBitcoinViaEmailEnabled(0);
        created.setAliasEmail(defaultAliasEmail(userId));
        created.setBalanceMasked(0);
        created.setAddressPrivacyEnabled(1);
        created.setAddressPoolSize(WalletParitySupport.DEFAULT_ADDRESS_POOL_SIZE);
        String phrase = walletParitySupport.generateRecoveryPhrase();
        created.setRecoveryPhrase(phrase);
        created.setRecoveryFingerprint(walletParitySupport.fingerprintOf(phrase + "#" + account.getId()));
        created.setPassphraseHint("");
        created.setLastRecoveryViewedAt(null);
        created.setCreatedAt(now);
        created.setUpdatedAt(now);
        created.setDeleted(0);
        walletAccountProfileMapper.insert(created);
        return created;
    }
    private List<WalletEmailTransferVo> listTransferVos(Long accountId, Long userId, int limit) {
        return walletEmailTransferMapper.selectList(new LambdaQueryWrapper<WalletEmailTransfer>()
                        .eq(WalletEmailTransfer::getOwnerId, userId)
                        .eq(WalletEmailTransfer::getAccountId, accountId)
                        .orderByDesc(WalletEmailTransfer::getCreatedAt)
                        .last("limit " + limit))
                .stream()
                .sorted(Comparator.comparing(WalletEmailTransfer::getCreatedAt).reversed())
                .map(walletParitySupport::toEmailTransferVo)
                .toList();
    }
    private WalletEmailTransfer createTransfer(
            WalletAccount account,
            WalletTransactionVo tx,
            String recipientEmail,
            String deliveryMessage
    ) {
        LocalDateTime now = LocalDateTime.now();
        WalletEmailTransfer transfer = new WalletEmailTransfer();
        transfer.setTransactionId(parseId(tx.transactionId(), "Wallet transactionId is invalid"));
        transfer.setAccountId(account.getId());
        transfer.setOwnerId(account.getOwnerId());
        transfer.setRecipientEmail(recipientEmail);
        transfer.setDeliveryMessage(deliveryMessage);
        transfer.setClaimCode(walletParitySupport.generateClaimCode());
        transfer.setStatus(TRANSFER_PENDING);
        transfer.setInviteRequired(userExists(recipientEmail) ? 0 : 1);
        transfer.setAmountMinor(tx.amountMinor());
        transfer.setAssetSymbol(tx.assetSymbol());
        transfer.setClaimedAt(null);
        transfer.setCreatedAt(now);
        transfer.setUpdatedAt(now);
        transfer.setDeleted(0);
        walletEmailTransferMapper.insert(transfer);
        return transfer;
    }
    private WalletAccount loadAccount(Long userId, Long accountId) {
        if (accountId == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Wallet accountId is required");
        }
        WalletAccount account = walletAccountMapper.selectOne(new LambdaQueryWrapper<WalletAccount>()
                .eq(WalletAccount::getId, accountId)
                .eq(WalletAccount::getOwnerId, userId));
        if (account == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Wallet account is not found");
        }
        return account;
    }
    private WalletEmailTransfer loadTransfer(Long userId, Long transferId) {
        if (transferId == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Wallet transferId is required");
        }
        WalletEmailTransfer transfer = walletEmailTransferMapper.selectOne(new LambdaQueryWrapper<WalletEmailTransfer>()
                .eq(WalletEmailTransfer::getId, transferId)
                .eq(WalletEmailTransfer::getOwnerId, userId));
        if (transfer == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Wallet email transfer is not found");
        }
        if (!TRANSFER_STATUSES.contains(transfer.getStatus())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Wallet email transfer status is invalid");
        }
        return transfer;
    }
    private void ensureBitcoinViaEmailAvailable(Long accountId, Long userId, String aliasEmail) {
        List<WalletAccountProfile> enabledProfiles = walletAccountProfileMapper.selectList(new LambdaQueryWrapper<WalletAccountProfile>()
                .eq(WalletAccountProfile::getOwnerId, userId)
                .eq(WalletAccountProfile::getBitcoinViaEmailEnabled, 1)
                .eq(WalletAccountProfile::getAliasEmail, aliasEmail));
        boolean conflict = enabledProfiles.stream().anyMatch(item -> !item.getAccountId().equals(accountId));
        if (conflict) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Wallet alias email is already bound to another Bitcoin account");
        }
    }
    private void requireBitcoinAccount(WalletAccount account) {
        if (!WALLET_ASSET_BTC.equalsIgnoreCase(account.getAssetSymbol())) throw new BizException(
                ErrorCode.INVALID_ARGUMENT, "Wallet Bitcoin via Email only supports BTC accounts");
    }
    private String requireImportAsset(String assetSymbol) {
        String normalized = walletParitySupport.normalizeAssetSymbol(assetSymbol);
        if (!WALLET_ASSET_BTC.equals(normalized)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Wallet import only supports BTC");
        }
        return normalized;
    }
    private void requireEmailEnabled(WalletAccountProfile profile) {
        if (profile.getBitcoinViaEmailEnabled() == null || profile.getBitcoinViaEmailEnabled() != 1)
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Wallet Bitcoin via Email is disabled");
    }
    private void requireAddressPrivacyEnabled(WalletAccountProfile profile) {
        if (profile.getAddressPrivacyEnabled() == null || profile.getAddressPrivacyEnabled() != 1)
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Wallet address privacy is disabled");
    }
    private int normalizeLimit(Integer limit) {
        return limit == null ? 10 : Math.max(1, Math.min(MAX_TRANSFER_LIMIT, limit));
    }
    private int resolveFlag(Boolean requested, Integer current) {
        return requested == null ? (current == null ? 0 : current) : (requested ? 1 : 0);
    }
    private String defaultAliasEmail(Long userId) {
        UserAccount user = userAccountMapper.selectById(userId);
        if (user == null || !StringUtils.hasText(user.getEmail())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Wallet alias email is unavailable");
        }
        return user.getEmail().trim().toLowerCase(Locale.ROOT);
    }
    private boolean userExists(String email) {
        return userAccountMapper.selectCount(new LambdaQueryWrapper<UserAccount>()
                .eq(UserAccount::getEmail, email.toLowerCase(Locale.ROOT))) > 0;
    }
    private Long parseId(String value, String message) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, message);
        }
    }
}
