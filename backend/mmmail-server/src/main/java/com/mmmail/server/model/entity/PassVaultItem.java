package com.mmmail.server.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("pass_vault_item")
public class PassVaultItem {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long ownerId;
    private Long orgId;
    private Long sharedVaultId;
    private String scopeType;
    private String itemType;
    private String title;
    private String website;
    private String username;
    private String secretCiphertext;
    private String twoFactorIssuer;
    private String twoFactorAccountName;
    private String twoFactorSecretCiphertext;
    private String twoFactorAlgorithm;
    private Integer twoFactorDigits;
    private Integer twoFactorPeriodSeconds;
    private String note;
    private Integer favorite;
    private Integer monitorExcluded;
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

    public Long getOrgId() {
        return orgId;
    }

    public void setOrgId(Long orgId) {
        this.orgId = orgId;
    }

    public Long getSharedVaultId() {
        return sharedVaultId;
    }

    public void setSharedVaultId(Long sharedVaultId) {
        this.sharedVaultId = sharedVaultId;
    }

    public String getScopeType() {
        return scopeType;
    }

    public void setScopeType(String scopeType) {
        this.scopeType = scopeType;
    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getSecretCiphertext() {
        return secretCiphertext;
    }

    public void setSecretCiphertext(String secretCiphertext) {
        this.secretCiphertext = secretCiphertext;
    }

    public String getTwoFactorIssuer() {
        return twoFactorIssuer;
    }

    public void setTwoFactorIssuer(String twoFactorIssuer) {
        this.twoFactorIssuer = twoFactorIssuer;
    }

    public String getTwoFactorAccountName() {
        return twoFactorAccountName;
    }

    public void setTwoFactorAccountName(String twoFactorAccountName) {
        this.twoFactorAccountName = twoFactorAccountName;
    }

    public String getTwoFactorSecretCiphertext() {
        return twoFactorSecretCiphertext;
    }

    public void setTwoFactorSecretCiphertext(String twoFactorSecretCiphertext) {
        this.twoFactorSecretCiphertext = twoFactorSecretCiphertext;
    }

    public String getTwoFactorAlgorithm() {
        return twoFactorAlgorithm;
    }

    public void setTwoFactorAlgorithm(String twoFactorAlgorithm) {
        this.twoFactorAlgorithm = twoFactorAlgorithm;
    }

    public Integer getTwoFactorDigits() {
        return twoFactorDigits;
    }

    public void setTwoFactorDigits(Integer twoFactorDigits) {
        this.twoFactorDigits = twoFactorDigits;
    }

    public Integer getTwoFactorPeriodSeconds() {
        return twoFactorPeriodSeconds;
    }

    public void setTwoFactorPeriodSeconds(Integer twoFactorPeriodSeconds) {
        this.twoFactorPeriodSeconds = twoFactorPeriodSeconds;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Integer getFavorite() {
        return favorite;
    }

    public void setFavorite(Integer favorite) {
        this.favorite = favorite;
    }

    public Integer getMonitorExcluded() {
        return monitorExcluded;
    }

    public void setMonitorExcluded(Integer monitorExcluded) {
        this.monitorExcluded = monitorExcluded;
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
