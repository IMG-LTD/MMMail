package com.mmmail.server.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("wallet_account_profile")
public class WalletAccountProfile {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long accountId;
    private Long ownerId;
    private Integer bitcoinViaEmailEnabled;
    private String aliasEmail;
    private Integer balanceMasked;
    private Integer addressPrivacyEnabled;
    private Integer addressPoolSize;
    private String recoveryPhrase;
    private String recoveryFingerprint;
    private String passphraseHint;
    private LocalDateTime lastRecoveryViewedAt;
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

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public Integer getBitcoinViaEmailEnabled() {
        return bitcoinViaEmailEnabled;
    }

    public void setBitcoinViaEmailEnabled(Integer bitcoinViaEmailEnabled) {
        this.bitcoinViaEmailEnabled = bitcoinViaEmailEnabled;
    }

    public String getAliasEmail() {
        return aliasEmail;
    }

    public void setAliasEmail(String aliasEmail) {
        this.aliasEmail = aliasEmail;
    }

    public Integer getBalanceMasked() {
        return balanceMasked;
    }

    public void setBalanceMasked(Integer balanceMasked) {
        this.balanceMasked = balanceMasked;
    }

    public Integer getAddressPrivacyEnabled() {
        return addressPrivacyEnabled;
    }

    public void setAddressPrivacyEnabled(Integer addressPrivacyEnabled) {
        this.addressPrivacyEnabled = addressPrivacyEnabled;
    }

    public Integer getAddressPoolSize() {
        return addressPoolSize;
    }

    public void setAddressPoolSize(Integer addressPoolSize) {
        this.addressPoolSize = addressPoolSize;
    }

    public String getRecoveryPhrase() {
        return recoveryPhrase;
    }

    public void setRecoveryPhrase(String recoveryPhrase) {
        this.recoveryPhrase = recoveryPhrase;
    }

    public String getRecoveryFingerprint() {
        return recoveryFingerprint;
    }

    public void setRecoveryFingerprint(String recoveryFingerprint) {
        this.recoveryFingerprint = recoveryFingerprint;
    }

    public String getPassphraseHint() {
        return passphraseHint;
    }

    public void setPassphraseHint(String passphraseHint) {
        this.passphraseHint = passphraseHint;
    }

    public LocalDateTime getLastRecoveryViewedAt() {
        return lastRecoveryViewedAt;
    }

    public void setLastRecoveryViewedAt(LocalDateTime lastRecoveryViewedAt) {
        this.lastRecoveryViewedAt = lastRecoveryViewedAt;
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
