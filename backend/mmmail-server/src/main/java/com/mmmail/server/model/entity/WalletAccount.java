package com.mmmail.server.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("wallet_account")
public class WalletAccount {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long ownerId;
    private String walletName;
    private String assetSymbol;
    private String address;
    private Long balanceMinor;
    private String addressType;
    private Integer accountIndex;
    private Integer imported;
    private String walletSourceFingerprint;
    private Integer walletPassphraseProtected;
    private LocalDateTime importedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic(value = "0", delval = "1")
    private Integer deleted;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public String getWalletName() {
        return walletName;
    }

    public void setWalletName(String walletName) {
        this.walletName = walletName;
    }

    public String getAssetSymbol() {
        return assetSymbol;
    }

    public void setAssetSymbol(String assetSymbol) {
        this.assetSymbol = assetSymbol;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Long getBalanceMinor() {
        return balanceMinor;
    }

    public void setBalanceMinor(Long balanceMinor) {
        this.balanceMinor = balanceMinor;
    }

    public String getAddressType() {
        return addressType;
    }

    public void setAddressType(String addressType) {
        this.addressType = addressType;
    }

    public Integer getAccountIndex() {
        return accountIndex;
    }

    public void setAccountIndex(Integer accountIndex) {
        this.accountIndex = accountIndex;
    }

    public Integer getImported() {
        return imported;
    }

    public void setImported(Integer imported) {
        this.imported = imported;
    }

    public String getWalletSourceFingerprint() {
        return walletSourceFingerprint;
    }

    public void setWalletSourceFingerprint(String walletSourceFingerprint) {
        this.walletSourceFingerprint = walletSourceFingerprint;
    }

    public Integer getWalletPassphraseProtected() {
        return walletPassphraseProtected;
    }

    public void setWalletPassphraseProtected(Integer walletPassphraseProtected) {
        this.walletPassphraseProtected = walletPassphraseProtected;
    }

    public LocalDateTime getImportedAt() {
        return importedAt;
    }

    public void setImportedAt(LocalDateTime importedAt) {
        this.importedAt = importedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }
}
