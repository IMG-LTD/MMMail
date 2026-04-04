package com.mmmail.server.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("drive_item")
public class DriveItem {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long ownerId;
    private Long parentId;
    private Long teamSpaceId;
    private String itemType;
    private String name;
    private String mimeType;
    private Long sizeBytes;
    private String storagePath;
    private String checksum;
    private Integer e2eeEnabled;
    private String e2eeAlgorithm;
    private String e2eeFingerprintsJson;
    private LocalDateTime trashedAt;
    private LocalDateTime purgeAfterAt;
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

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public Long getTeamSpaceId() {
        return teamSpaceId;
    }

    public void setTeamSpaceId(Long teamSpaceId) {
        this.teamSpaceId = teamSpaceId;
    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public LocalDateTime getTrashedAt() {
        return trashedAt;
    }

    public void setTrashedAt(LocalDateTime trashedAt) {
        this.trashedAt = trashedAt;
    }

    public LocalDateTime getPurgeAfterAt() {
        return purgeAfterAt;
    }

    public void setPurgeAfterAt(LocalDateTime purgeAfterAt) {
        this.purgeAfterAt = purgeAfterAt;
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
