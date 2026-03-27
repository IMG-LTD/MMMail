package com.mmmail.server.model.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("mail_filter")
public class MailFilter {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long ownerId;
    private String name;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String senderContains;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String subjectContains;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String keywordContains;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String targetFolder;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long targetCustomFolderId;
    private String labelsJson;
    private Integer markRead;
    private Integer enabled;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSenderContains() {
        return senderContains;
    }

    public void setSenderContains(String senderContains) {
        this.senderContains = senderContains;
    }

    public String getSubjectContains() {
        return subjectContains;
    }

    public void setSubjectContains(String subjectContains) {
        this.subjectContains = subjectContains;
    }

    public String getKeywordContains() {
        return keywordContains;
    }

    public void setKeywordContains(String keywordContains) {
        this.keywordContains = keywordContains;
    }

    public String getTargetFolder() {
        return targetFolder;
    }

    public void setTargetFolder(String targetFolder) {
        this.targetFolder = targetFolder;
    }

    public Long getTargetCustomFolderId() {
        return targetCustomFolderId;
    }

    public void setTargetCustomFolderId(Long targetCustomFolderId) {
        this.targetCustomFolderId = targetCustomFolderId;
    }

    public String getLabelsJson() {
        return labelsJson;
    }

    public void setLabelsJson(String labelsJson) {
        this.labelsJson = labelsJson;
    }

    public Integer getMarkRead() {
        return markRead;
    }

    public void setMarkRead(Integer markRead) {
        this.markRead = markRead;
    }

    public Integer getEnabled() {
        return enabled;
    }

    public void setEnabled(Integer enabled) {
        this.enabled = enabled;
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
