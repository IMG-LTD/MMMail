package com.mmmail.server.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("suite_governance_request")
public class SuiteGovernanceRequest {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long ownerId;
    private Long orgId;
    private String requestId;
    private String templateCode;
    private String templateName;
    private String status;
    private String reason;
    private Integer requireDualReview;
    private String firstReviewNote;
    private Long firstReviewedByUserId;
    private Long firstReviewedBySessionId;
    private Long secondReviewerUserId;
    private String reviewNote;
    private Long reviewedByUserId;
    private Long reviewedBySessionId;
    private String approvalNote;
    private Long executedByUserId;
    private Long executedBySessionId;
    private String rollbackReason;
    private LocalDateTime requestedAt;
    private LocalDateTime reviewDueAt;
    private LocalDateTime firstReviewedAt;
    private LocalDateTime reviewedAt;
    private LocalDateTime approvedAt;
    private LocalDateTime executedAt;
    private LocalDateTime rolledBackAt;
    private String actionCodesJson;
    private String rollbackActionCodesJson;
    private String executionResultsJson;
    private String rollbackResultsJson;
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

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getTemplateCode() {
        return templateCode;
    }

    public void setTemplateCode(String templateCode) {
        this.templateCode = templateCode;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Integer getRequireDualReview() {
        return requireDualReview;
    }

    public void setRequireDualReview(Integer requireDualReview) {
        this.requireDualReview = requireDualReview;
    }

    public String getFirstReviewNote() {
        return firstReviewNote;
    }

    public void setFirstReviewNote(String firstReviewNote) {
        this.firstReviewNote = firstReviewNote;
    }

    public Long getFirstReviewedByUserId() {
        return firstReviewedByUserId;
    }

    public void setFirstReviewedByUserId(Long firstReviewedByUserId) {
        this.firstReviewedByUserId = firstReviewedByUserId;
    }

    public Long getFirstReviewedBySessionId() {
        return firstReviewedBySessionId;
    }

    public void setFirstReviewedBySessionId(Long firstReviewedBySessionId) {
        this.firstReviewedBySessionId = firstReviewedBySessionId;
    }

    public Long getSecondReviewerUserId() {
        return secondReviewerUserId;
    }

    public void setSecondReviewerUserId(Long secondReviewerUserId) {
        this.secondReviewerUserId = secondReviewerUserId;
    }

    public String getReviewNote() {
        return reviewNote;
    }

    public void setReviewNote(String reviewNote) {
        this.reviewNote = reviewNote;
    }

    public String getApprovalNote() {
        return approvalNote;
    }

    public void setApprovalNote(String approvalNote) {
        this.approvalNote = approvalNote;
    }

    public Long getReviewedByUserId() {
        return reviewedByUserId;
    }

    public void setReviewedByUserId(Long reviewedByUserId) {
        this.reviewedByUserId = reviewedByUserId;
    }

    public Long getReviewedBySessionId() {
        return reviewedBySessionId;
    }

    public void setReviewedBySessionId(Long reviewedBySessionId) {
        this.reviewedBySessionId = reviewedBySessionId;
    }

    public Long getExecutedByUserId() {
        return executedByUserId;
    }

    public void setExecutedByUserId(Long executedByUserId) {
        this.executedByUserId = executedByUserId;
    }

    public Long getExecutedBySessionId() {
        return executedBySessionId;
    }

    public void setExecutedBySessionId(Long executedBySessionId) {
        this.executedBySessionId = executedBySessionId;
    }

    public String getRollbackReason() {
        return rollbackReason;
    }

    public void setRollbackReason(String rollbackReason) {
        this.rollbackReason = rollbackReason;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(LocalDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }

    public LocalDateTime getReviewDueAt() {
        return reviewDueAt;
    }

    public void setReviewDueAt(LocalDateTime reviewDueAt) {
        this.reviewDueAt = reviewDueAt;
    }

    public LocalDateTime getFirstReviewedAt() {
        return firstReviewedAt;
    }

    public void setFirstReviewedAt(LocalDateTime firstReviewedAt) {
        this.firstReviewedAt = firstReviewedAt;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }

    public LocalDateTime getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(LocalDateTime executedAt) {
        this.executedAt = executedAt;
    }

    public LocalDateTime getRolledBackAt() {
        return rolledBackAt;
    }

    public void setRolledBackAt(LocalDateTime rolledBackAt) {
        this.rolledBackAt = rolledBackAt;
    }

    public String getActionCodesJson() {
        return actionCodesJson;
    }

    public void setActionCodesJson(String actionCodesJson) {
        this.actionCodesJson = actionCodesJson;
    }

    public String getRollbackActionCodesJson() {
        return rollbackActionCodesJson;
    }

    public void setRollbackActionCodesJson(String rollbackActionCodesJson) {
        this.rollbackActionCodesJson = rollbackActionCodesJson;
    }

    public String getExecutionResultsJson() {
        return executionResultsJson;
    }

    public void setExecutionResultsJson(String executionResultsJson) {
        this.executionResultsJson = executionResultsJson;
    }

    public String getRollbackResultsJson() {
        return rollbackResultsJson;
    }

    public void setRollbackResultsJson(String rollbackResultsJson) {
        this.rollbackResultsJson = rollbackResultsJson;
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
