package com.mmmail.server.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("suite_notification_operation_log")
public class SuiteNotificationOperationLog {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long ownerId;
    private String operationId;
    private String notificationId;
    private String previousWorkflowStatus;
    private LocalDateTime previousSnoozedUntil;
    private Long previousAssignedToUserId;
    private String previousAssignedToDisplayName;
    private Integer undone;
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

    public String getOperationId() {
        return operationId;
    }

    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }

    public String getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(String notificationId) {
        this.notificationId = notificationId;
    }

    public String getPreviousWorkflowStatus() {
        return previousWorkflowStatus;
    }

    public void setPreviousWorkflowStatus(String previousWorkflowStatus) {
        this.previousWorkflowStatus = previousWorkflowStatus;
    }

    public LocalDateTime getPreviousSnoozedUntil() {
        return previousSnoozedUntil;
    }

    public void setPreviousSnoozedUntil(LocalDateTime previousSnoozedUntil) {
        this.previousSnoozedUntil = previousSnoozedUntil;
    }

    public Long getPreviousAssignedToUserId() {
        return previousAssignedToUserId;
    }

    public void setPreviousAssignedToUserId(Long previousAssignedToUserId) {
        this.previousAssignedToUserId = previousAssignedToUserId;
    }

    public String getPreviousAssignedToDisplayName() {
        return previousAssignedToDisplayName;
    }

    public void setPreviousAssignedToDisplayName(String previousAssignedToDisplayName) {
        this.previousAssignedToDisplayName = previousAssignedToDisplayName;
    }

    public Integer getUndone() {
        return undone;
    }

    public void setUndone(Integer undone) {
        this.undone = undone;
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
