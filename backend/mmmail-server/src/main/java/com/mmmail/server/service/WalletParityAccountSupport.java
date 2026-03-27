package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.mapper.WalletAccountMapper;
import com.mmmail.server.mapper.WalletAccountProfileMapper;
import com.mmmail.server.mapper.WalletReceiveAddressMapper;
import com.mmmail.server.model.entity.WalletAccount;
import com.mmmail.server.model.entity.WalletAccountProfile;
import com.mmmail.server.model.entity.WalletReceiveAddress;
import com.mmmail.server.model.vo.WalletReceiveAddressVo;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Component
public class WalletParityAccountSupport {

    private static final String CHANGE_RESERVED_FOR = "Internal change flow";

    private final WalletAccountMapper walletAccountMapper;
    private final WalletAccountProfileMapper walletAccountProfileMapper;
    private final WalletReceiveAddressMapper walletReceiveAddressMapper;
    private final WalletParitySupport walletParitySupport;

    public WalletParityAccountSupport(
            WalletAccountMapper walletAccountMapper,
            WalletAccountProfileMapper walletAccountProfileMapper,
            WalletReceiveAddressMapper walletReceiveAddressMapper,
            WalletParitySupport walletParitySupport
    ) {
        this.walletAccountMapper = walletAccountMapper;
        this.walletAccountProfileMapper = walletAccountProfileMapper;
        this.walletReceiveAddressMapper = walletReceiveAddressMapper;
        this.walletParitySupport = walletParitySupport;
    }

    public void ensureAddressInventory(WalletAccount account) {
        syncBaseAddresses(account);
    }

    public List<WalletReceiveAddressVo> listAddressVos(WalletAccount account, String addressKind) {
        return listAddresses(account.getId(), account.getOwnerId(), addressKind).stream()
                .map(item -> toAddressVo(account, item))
                .toList();
    }

    public List<WalletReceiveAddressVo> filterAddresses(List<WalletReceiveAddressVo> addresses, String query) {
        if (!StringUtils.hasText(query)) {
            return addresses;
        }
        return addresses.stream()
                .filter(item -> contains(item.address(), query)
                        || contains(item.label(), query)
                        || contains(item.reservedFor(), query))
                .toList();
    }

    public int countTrailingUnused(List<WalletReceiveAddressVo> addresses) {
        int count = 0;
        for (int index = addresses.size() - 1; index >= 0; index -= 1) {
            if (!WalletParitySupport.ADDRESS_STATUS_UNUSED.equals(addresses.get(index).addressStatus())) {
                break;
            }
            count += 1;
        }
        return count;
    }

    public WalletReceiveAddressVo createRotatedReceiveAddress(WalletAccount account, String label) {
        ensureAddressInventory(account);
        int nextIndex = nextAddressIndex(account.getId(), account.getOwnerId(), WalletParitySupport.ADDRESS_KIND_RECEIVE);
        WalletReceiveAddress address = createAddress(
                account,
                label,
                WalletParitySupport.ADDRESS_SOURCE_ROTATED,
                WalletParitySupport.ADDRESS_KIND_RECEIVE,
                nextIndex,
                WalletParitySupport.ADDRESS_STATUS_UNUSED,
                0L,
                null,
                walletParitySupport.generateDerivedAddress(
                        resolveAddressType(account),
                        resolveAccountIndex(account),
                        WalletParitySupport.ADDRESS_KIND_RECEIVE,
                        nextIndex,
                        resolveAccountSeedFingerprint(account)
                )
        );
        return toAddressVo(account, address);
    }

    public void ensureAccountSlotAvailable(
            Long userId,
            Long currentAccountId,
            String assetSymbol,
            String addressType,
            int accountIndex
    ) {
        LambdaQueryWrapper<WalletAccount> query = new LambdaQueryWrapper<WalletAccount>()
                .eq(WalletAccount::getOwnerId, userId)
                .eq(WalletAccount::getAssetSymbol, assetSymbol)
                .eq(WalletAccount::getAddressType, addressType)
                .eq(WalletAccount::getAccountIndex, accountIndex);
        if (currentAccountId != null) {
            query.ne(WalletAccount::getId, currentAccountId);
        }
        WalletAccount existing = walletAccountMapper.selectOne(query);
        if (existing != null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Wallet address type and account index already exist");
        }
    }

    public void applyAdvancedSettings(WalletAccount account, String walletName, String addressType, int accountIndex) {
        account.setWalletName(walletName);
        account.setAddressType(addressType);
        account.setAccountIndex(accountIndex);
        account.setAddress(walletParitySupport.generateAccountAddress(
                addressType,
                accountIndex,
                resolveAccountSeedFingerprint(account)
        ));
        account.setUpdatedAt(LocalDateTime.now());
    }

    public WalletAccount createImportedAccount(
            Long userId,
            String walletName,
            String assetSymbol,
            String addressType,
            int accountIndex,
            String sourceFingerprint,
            boolean passphraseProtected
    ) {
        LocalDateTime now = LocalDateTime.now();
        WalletAccount account = new WalletAccount();
        account.setOwnerId(userId);
        account.setWalletName(walletName);
        account.setAssetSymbol(assetSymbol);
        account.setAddressType(addressType);
        account.setAccountIndex(accountIndex);
        account.setImported(1);
        account.setWalletSourceFingerprint(sourceFingerprint);
        account.setWalletPassphraseProtected(passphraseProtected ? 1 : 0);
        account.setImportedAt(now);
        account.setAddress(walletParitySupport.generateAccountAddress(addressType, accountIndex, sourceFingerprint));
        account.setBalanceMinor(0L);
        account.setCreatedAt(now);
        account.setUpdatedAt(now);
        account.setDeleted(0);
        return account;
    }

    public void createImportedProfile(Long userId, WalletAccount account, String sourceFingerprint, String aliasEmail) {
        LocalDateTime now = LocalDateTime.now();
        WalletAccountProfile profile = new WalletAccountProfile();
        profile.setAccountId(account.getId());
        profile.setOwnerId(userId);
        profile.setBitcoinViaEmailEnabled(0);
        profile.setAliasEmail(aliasEmail);
        profile.setBalanceMasked(0);
        profile.setAddressPrivacyEnabled(1);
        profile.setAddressPoolSize(WalletParitySupport.DEFAULT_ADDRESS_POOL_SIZE);
        profile.setRecoveryPhrase("");
        profile.setRecoveryFingerprint(sourceFingerprint);
        profile.setPassphraseHint("");
        profile.setLastRecoveryViewedAt(null);
        profile.setCreatedAt(now);
        profile.setUpdatedAt(now);
        profile.setDeleted(0);
        walletAccountProfileMapper.insert(profile);
    }

    private void syncBaseAddresses(WalletAccount account) {
        upsertBaseAddress(
                account,
                WalletParitySupport.ADDRESS_KIND_RECEIVE,
                "Primary address",
                WalletParitySupport.ADDRESS_SOURCE_PRIMARY,
                null
        );
        upsertBaseAddress(
                account,
                WalletParitySupport.ADDRESS_KIND_CHANGE,
                "Change 0",
                WalletParitySupport.ADDRESS_SOURCE_INTERNAL,
                CHANGE_RESERVED_FOR
        );
    }

    private void upsertBaseAddress(
            WalletAccount account,
            String addressKind,
            String label,
            String sourceType,
            String reservedFor
    ) {
        WalletReceiveAddress existing = findAddress(account.getId(), account.getOwnerId(), addressKind, 0);
        String address = WalletParitySupport.ADDRESS_KIND_RECEIVE.equals(addressKind)
                ? account.getAddress()
                : walletParitySupport.generateDerivedAddress(
                        resolveAddressType(account),
                        resolveAccountIndex(account),
                        WalletParitySupport.ADDRESS_KIND_CHANGE,
                        0,
                        resolveAccountSeedFingerprint(account)
                );
        if (existing == null) {
            createAddress(
                    account,
                    label,
                    sourceType,
                    addressKind,
                    0,
                    WalletParitySupport.ADDRESS_STATUS_UNUSED,
                    0L,
                    reservedFor,
                    address
            );
            return;
        }
        existing.setAddress(address);
        existing.setLabel(label);
        existing.setSourceType(sourceType);
        existing.setAddressKind(addressKind);
        existing.setAddressIndex(0);
        existing.setAddressStatus(WalletParitySupport.ADDRESS_STATUS_UNUSED);
        existing.setReservedFor(reservedFor);
        existing.setUpdatedAt(LocalDateTime.now());
        walletReceiveAddressMapper.updateById(existing);
    }

    private WalletReceiveAddress createAddress(
            WalletAccount account,
            String label,
            String sourceType,
            String addressKind,
            int addressIndex,
            String addressStatus,
            long valueMinor,
            String reservedFor,
            String address
    ) {
        LocalDateTime now = LocalDateTime.now();
        WalletReceiveAddress item = new WalletReceiveAddress();
        item.setAccountId(account.getId());
        item.setOwnerId(account.getOwnerId());
        item.setAddress(address);
        item.setLabel(label);
        item.setSourceType(sourceType);
        item.setAddressKind(addressKind);
        item.setAddressIndex(addressIndex);
        item.setAddressStatus(addressStatus);
        item.setValueMinor(valueMinor);
        item.setReservedFor(reservedFor);
        item.setCreatedAt(now);
        item.setUpdatedAt(now);
        item.setDeleted(0);
        walletReceiveAddressMapper.insert(item);
        return item;
    }

    private List<WalletReceiveAddress> listAddresses(Long accountId, Long userId, String addressKind) {
        return walletReceiveAddressMapper.selectList(new LambdaQueryWrapper<WalletReceiveAddress>()
                        .eq(WalletReceiveAddress::getOwnerId, userId)
                        .eq(WalletReceiveAddress::getAccountId, accountId)
                        .eq(WalletReceiveAddress::getAddressKind, addressKind)
                        .orderByAsc(WalletReceiveAddress::getAddressIndex)
                        .orderByAsc(WalletReceiveAddress::getCreatedAt))
                .stream()
                .toList();
    }

    private WalletReceiveAddressVo toAddressVo(WalletAccount account, WalletReceiveAddress address) {
        return walletParitySupport.toReceiveAddressVo(address, resolveAddressValue(account, address));
    }

    private long resolveAddressValue(WalletAccount account, WalletReceiveAddress address) {
        long storedValue = address.getValueMinor() == null ? 0L : address.getValueMinor();
        if (storedValue > 0) {
            return storedValue;
        }
        boolean primaryReceive = WalletParitySupport.ADDRESS_KIND_RECEIVE.equals(address.getAddressKind())
                && address.getAddressIndex() != null
                && address.getAddressIndex() == 0;
        if (!primaryReceive) {
            return 0L;
        }
        return account.getBalanceMinor() == null ? 0L : account.getBalanceMinor();
    }

    private WalletReceiveAddress findAddress(Long accountId, Long ownerId, String addressKind, int addressIndex) {
        return walletReceiveAddressMapper.selectOne(new LambdaQueryWrapper<WalletReceiveAddress>()
                .eq(WalletReceiveAddress::getOwnerId, ownerId)
                .eq(WalletReceiveAddress::getAccountId, accountId)
                .eq(WalletReceiveAddress::getAddressKind, addressKind)
                .eq(WalletReceiveAddress::getAddressIndex, addressIndex));
    }

    private int nextAddressIndex(Long accountId, Long userId, String addressKind) {
        return listAddresses(accountId, userId, addressKind).stream()
                .map(WalletReceiveAddress::getAddressIndex)
                .filter(index -> index != null)
                .max(Integer::compareTo)
                .orElse(-1) + 1;
    }

    private String resolveAccountSeedFingerprint(WalletAccount account) {
        if (StringUtils.hasText(account.getWalletSourceFingerprint())) {
            return account.getWalletSourceFingerprint();
        }
        return walletParitySupport.fingerprintOf("wallet-account#" + account.getId() + "|" + account.getOwnerId());
    }

    private String resolveAddressType(WalletAccount account) {
        return walletParitySupport.normalizeAddressType(
                account.getAddressType(),
                WalletParitySupport.ADDRESS_TYPE_NATIVE_SEGWIT
        );
    }

    private int resolveAccountIndex(WalletAccount account) {
        return walletParitySupport.normalizeAccountIndex(
                account.getAccountIndex(),
                WalletParitySupport.DEFAULT_ACCOUNT_INDEX
        );
    }

    private boolean contains(String value, String query) {
        return StringUtils.hasText(value) && value.toLowerCase(Locale.ROOT).contains(query);
    }
}
