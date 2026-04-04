package com.mmmail.server.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("drive_file_version")
public class DriveFileVersion {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long ownerId;
    private Long itemId;
    private Integer versionNo;
    private String mimeType;
    private Long sizeBytes;
    private String storagePath;
    private String checksum;
    private Integer e2eeEnabled;
    private String e2eeAlgorithm;
    private String e2eeFingerprintsJson;
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

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public Integer getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(Integer versionNo) {
        this.versionNo = versionNo;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public Long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(Long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
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
