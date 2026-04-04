package com.mmmail.server.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("user_preference")
public class UserPreference {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long ownerId;
    private String signature;
    private String timezone;
    private String preferredLocale;
    private String mailAddressMode;
    private Integer autoSaveSeconds;
    private Integer undoSendSeconds;
    private Integer driveVersionRetentionCount;
    private Integer driveVersionRetentionDays;
    private Integer authenticatorSyncEnabled;
    private Integer authenticatorEncryptedBackupEnabled;
    private Integer authenticatorPinProtectionEnabled;
    private String authenticatorPinHash;
    private Integer authenticatorLockTimeoutSeconds;
    private LocalDateTime authenticatorLastSyncedAt;
    private LocalDateTime authenticatorLastBackupAt;
    private String vpnNetshieldMode;
    private Integer vpnKillSwitchEnabled;
    private String vpnDefaultConnectionMode;
    private Long vpnDefaultProfileId;
    private Integer mailE2eeEnabled;
    private String mailE2eeKeyFingerprint;
    private String mailE2eePublicKeyArmored;
    private String mailE2eePrivateKeyEncrypted;
    private String mailE2eeKeyAlgorithm;
    private LocalDateTime mailE2eeKeyCreatedAt;
    private String mailE2eeRecoveryPrivateKeyEncrypted;
    private LocalDateTime mailE2eeRecoveryUpdatedAt;
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

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getPreferredLocale() {
        return preferredLocale;
    }

    public void setPreferredLocale(String preferredLocale) {
        this.preferredLocale = preferredLocale;
    }

    public String getMailAddressMode() {
        return mailAddressMode;
    }

    public void setMailAddressMode(String mailAddressMode) {
        this.mailAddressMode = mailAddressMode;
    }

    public Integer getAutoSaveSeconds() {
        return autoSaveSeconds;
    }

    public void setAutoSaveSeconds(Integer autoSaveSeconds) {
        this.autoSaveSeconds = autoSaveSeconds;
    }

    public Integer getUndoSendSeconds() {
        return undoSendSeconds;
    }

    public void setUndoSendSeconds(Integer undoSendSeconds) {
        this.undoSendSeconds = undoSendSeconds;
    }

    public Integer getDriveVersionRetentionCount() {
        return driveVersionRetentionCount;
    }

    public void setDriveVersionRetentionCount(Integer driveVersionRetentionCount) {
        this.driveVersionRetentionCount = driveVersionRetentionCount;
    }

    public Integer getDriveVersionRetentionDays() {
        return driveVersionRetentionDays;
    }

    public void setDriveVersionRetentionDays(Integer driveVersionRetentionDays) {
        this.driveVersionRetentionDays = driveVersionRetentionDays;
    }

    public Integer getAuthenticatorSyncEnabled() {
        return authenticatorSyncEnabled;
    }

    public void setAuthenticatorSyncEnabled(Integer authenticatorSyncEnabled) {
        this.authenticatorSyncEnabled = authenticatorSyncEnabled;
    }

    public Integer getAuthenticatorEncryptedBackupEnabled() {
        return authenticatorEncryptedBackupEnabled;
    }

    public void setAuthenticatorEncryptedBackupEnabled(Integer authenticatorEncryptedBackupEnabled) {
        this.authenticatorEncryptedBackupEnabled = authenticatorEncryptedBackupEnabled;
    }

    public Integer getAuthenticatorPinProtectionEnabled() {
        return authenticatorPinProtectionEnabled;
    }

    public void setAuthenticatorPinProtectionEnabled(Integer authenticatorPinProtectionEnabled) {
        this.authenticatorPinProtectionEnabled = authenticatorPinProtectionEnabled;
    }

    public String getAuthenticatorPinHash() {
        return authenticatorPinHash;
    }

    public void setAuthenticatorPinHash(String authenticatorPinHash) {
        this.authenticatorPinHash = authenticatorPinHash;
    }

    public Integer getAuthenticatorLockTimeoutSeconds() {
        return authenticatorLockTimeoutSeconds;
    }

    public void setAuthenticatorLockTimeoutSeconds(Integer authenticatorLockTimeoutSeconds) {
        this.authenticatorLockTimeoutSeconds = authenticatorLockTimeoutSeconds;
    }

    public LocalDateTime getAuthenticatorLastSyncedAt() {
        return authenticatorLastSyncedAt;
    }

    public void setAuthenticatorLastSyncedAt(LocalDateTime authenticatorLastSyncedAt) {
        this.authenticatorLastSyncedAt = authenticatorLastSyncedAt;
    }

    public LocalDateTime getAuthenticatorLastBackupAt() {
        return authenticatorLastBackupAt;
    }

    public void setAuthenticatorLastBackupAt(LocalDateTime authenticatorLastBackupAt) {
        this.authenticatorLastBackupAt = authenticatorLastBackupAt;
    }

    public String getVpnNetshieldMode() {
        return vpnNetshieldMode;
    }

    public void setVpnNetshieldMode(String vpnNetshieldMode) {
        this.vpnNetshieldMode = vpnNetshieldMode;
    }

    public Integer getVpnKillSwitchEnabled() {
        return vpnKillSwitchEnabled;
    }

    public void setVpnKillSwitchEnabled(Integer vpnKillSwitchEnabled) {
        this.vpnKillSwitchEnabled = vpnKillSwitchEnabled;
    }

    public String getVpnDefaultConnectionMode() {
        return vpnDefaultConnectionMode;
    }

    public void setVpnDefaultConnectionMode(String vpnDefaultConnectionMode) {
        this.vpnDefaultConnectionMode = vpnDefaultConnectionMode;
    }

    public Long getVpnDefaultProfileId() {
        return vpnDefaultProfileId;
    }

    public void setVpnDefaultProfileId(Long vpnDefaultProfileId) {
        this.vpnDefaultProfileId = vpnDefaultProfileId;
    }

    public Integer getMailE2eeEnabled() {
        return mailE2eeEnabled;
    }

    public void setMailE2eeEnabled(Integer mailE2eeEnabled) {
        this.mailE2eeEnabled = mailE2eeEnabled;
    }

    public String getMailE2eeKeyFingerprint() {
        return mailE2eeKeyFingerprint;
    }

    public void setMailE2eeKeyFingerprint(String mailE2eeKeyFingerprint) {
        this.mailE2eeKeyFingerprint = mailE2eeKeyFingerprint;
    }

    public String getMailE2eePublicKeyArmored() {
        return mailE2eePublicKeyArmored;
    }

    public void setMailE2eePublicKeyArmored(String mailE2eePublicKeyArmored) {
        this.mailE2eePublicKeyArmored = mailE2eePublicKeyArmored;
    }

    public String getMailE2eePrivateKeyEncrypted() {
        return mailE2eePrivateKeyEncrypted;
    }

    public void setMailE2eePrivateKeyEncrypted(String mailE2eePrivateKeyEncrypted) {
        this.mailE2eePrivateKeyEncrypted = mailE2eePrivateKeyEncrypted;
    }

    public String getMailE2eeKeyAlgorithm() {
        return mailE2eeKeyAlgorithm;
    }

    public void setMailE2eeKeyAlgorithm(String mailE2eeKeyAlgorithm) {
        this.mailE2eeKeyAlgorithm = mailE2eeKeyAlgorithm;
    }

    public LocalDateTime getMailE2eeKeyCreatedAt() {
        return mailE2eeKeyCreatedAt;
    }

    public void setMailE2eeKeyCreatedAt(LocalDateTime mailE2eeKeyCreatedAt) {
        this.mailE2eeKeyCreatedAt = mailE2eeKeyCreatedAt;
    }

    public String getMailE2eeRecoveryPrivateKeyEncrypted() {
        return mailE2eeRecoveryPrivateKeyEncrypted;
    }

    public void setMailE2eeRecoveryPrivateKeyEncrypted(String mailE2eeRecoveryPrivateKeyEncrypted) {
        this.mailE2eeRecoveryPrivateKeyEncrypted = mailE2eeRecoveryPrivateKeyEncrypted;
    }

    public LocalDateTime getMailE2eeRecoveryUpdatedAt() {
        return mailE2eeRecoveryUpdatedAt;
    }

    public void setMailE2eeRecoveryUpdatedAt(LocalDateTime mailE2eeRecoveryUpdatedAt) {
        this.mailE2eeRecoveryUpdatedAt = mailE2eeRecoveryUpdatedAt;
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
