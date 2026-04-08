package com.mmmail.server.model.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("mail_message")
public class MailMessage {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long ownerId;
    private Long peerId;
    private String peerEmail;
    private String senderEmail;
    private String direction;
    private String folderType;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long customFolderId;
    private String subject;
    private String bodyCiphertext;
    private Integer bodyE2eeEnabled;
    private String bodyE2eeAlgorithm;
    private String bodyE2eeFingerprintsJson;
    private String bodyE2eeExternalAccessJson;
    private Integer isRead;
    private Integer isStarred;
    private Integer isDraft;
    private String labelsJson;
    private String deliveryTargetsJson;
    private String idempotencyKey;
    private LocalDateTime sentAt;
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

    public Long getPeerId() {
        return peerId;
    }

    public void setPeerId(Long peerId) {
        this.peerId = peerId;
    }

    public String getPeerEmail() {
        return peerEmail;
    }

    public void setPeerEmail(String peerEmail) {
        this.peerEmail = peerEmail;
    }

    public String getSenderEmail() {
        return senderEmail;
    }

    public void setSenderEmail(String senderEmail) {
        this.senderEmail = senderEmail;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public String getFolderType() {
        return folderType;
    }

    public void setFolderType(String folderType) {
        this.folderType = folderType;
    }

    public Long getCustomFolderId() {
        return customFolderId;
    }

    public void setCustomFolderId(Long customFolderId) {
        this.customFolderId = customFolderId;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBodyCiphertext() {
        return bodyCiphertext;
    }

    public void setBodyCiphertext(String bodyCiphertext) {
        this.bodyCiphertext = bodyCiphertext;
    }

    public Integer getBodyE2eeEnabled() {
        return bodyE2eeEnabled;
    }

    public void setBodyE2eeEnabled(Integer bodyE2eeEnabled) {
        this.bodyE2eeEnabled = bodyE2eeEnabled;
    }

    public String getBodyE2eeAlgorithm() {
        return bodyE2eeAlgorithm;
    }

    public void setBodyE2eeAlgorithm(String bodyE2eeAlgorithm) {
        this.bodyE2eeAlgorithm = bodyE2eeAlgorithm;
    }

    public String getBodyE2eeFingerprintsJson() {
        return bodyE2eeFingerprintsJson;
    }

    public void setBodyE2eeFingerprintsJson(String bodyE2eeFingerprintsJson) {
        this.bodyE2eeFingerprintsJson = bodyE2eeFingerprintsJson;
    }

    public String getBodyE2eeExternalAccessJson() {
        return bodyE2eeExternalAccessJson;
    }

    public void setBodyE2eeExternalAccessJson(String bodyE2eeExternalAccessJson) {
        this.bodyE2eeExternalAccessJson = bodyE2eeExternalAccessJson;
    }

    public Integer getIsRead() {
        return isRead;
    }

    public void setIsRead(Integer isRead) {
        this.isRead = isRead;
    }

    public Integer getIsDraft() {
        return isDraft;
    }

    public void setIsDraft(Integer isDraft) {
        this.isDraft = isDraft;
    }

    public Integer getIsStarred() {
        return isStarred;
    }

    public void setIsStarred(Integer isStarred) {
        this.isStarred = isStarred;
    }

    public String getLabelsJson() {
        return labelsJson;
    }

    public void setLabelsJson(String labelsJson) {
        this.labelsJson = labelsJson;
    }

    public String getDeliveryTargetsJson() {
        return deliveryTargetsJson;
    }

    public void setDeliveryTargetsJson(String deliveryTargetsJson) {
        this.deliveryTargetsJson = deliveryTargetsJson;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
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
