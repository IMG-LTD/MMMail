package com.mmmail.server.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("simplelogin_relay_policy")
public class SimpleLoginRelayPolicy {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long orgId;
    private Long customDomainId;
    private Long ownerId;
    private Integer catchAllEnabled;
    private String subdomainMode;
    private Long defaultMailboxId;
    private String defaultMailboxEmail;
    private String note;
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

    public Long getOrgId() {
        return orgId;
    }

    public void setOrgId(Long orgId) {
        this.orgId = orgId;
    }

    public Long getCustomDomainId() {
        return customDomainId;
    }

    public void setCustomDomainId(Long customDomainId) {
        this.customDomainId = customDomainId;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public Integer getCatchAllEnabled() {
        return catchAllEnabled;
    }

    public void setCatchAllEnabled(Integer catchAllEnabled) {
        this.catchAllEnabled = catchAllEnabled;
    }

    public String getSubdomainMode() {
        return subdomainMode;
    }

    public void setSubdomainMode(String subdomainMode) {
        this.subdomainMode = subdomainMode;
    }

    public Long getDefaultMailboxId() {
        return defaultMailboxId;
    }

    public void setDefaultMailboxId(Long defaultMailboxId) {
        this.defaultMailboxId = defaultMailboxId;
    }

    public String getDefaultMailboxEmail() {
        return defaultMailboxEmail;
    }

    public void setDefaultMailboxEmail(String defaultMailboxEmail) {
        this.defaultMailboxEmail = defaultMailboxEmail;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
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
