package com.mmmail.server.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("mail_attachment")
public class MailAttachment {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long ownerId;
    private Long mailId;
    private String fileName;
    private String contentType;
    private Long fileSize;
    private Integer e2eeEnabled;
    private String e2eeAlgorithm;
    private String e2eeFingerprintsJson;
    private String storagePath;
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

    public Long getMailId() {
        return mailId;
    }

    public void setMailId(Long mailId) {
        this.mailId = mailId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public Integer getE2eeEnabled() {
        return e2eeEnabled;
    }

    public void setE2eeEnabled(Integer e2eeEnabled) {
        this.e2eeEnabled = e2eeEnabled;
    }

    public String getE2eeAlgorithm() {
        return e2eeAlgorithm;
    }

    public void setE2eeAlgorithm(String e2eeAlgorithm) {
        this.e2eeAlgorithm = e2eeAlgorithm;
    }

    public String getE2eeFingerprintsJson() {
        return e2eeFingerprintsJson;
    }

    public void setE2eeFingerprintsJson(String e2eeFingerprintsJson) {
        this.e2eeFingerprintsJson = e2eeFingerprintsJson;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
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
