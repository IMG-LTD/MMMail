package com.mmmail.server.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("meet_access_enrollment")
public class MeetAccessEnrollment {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long ownerId;
    private String planCodeSnapshot;
    private Integer waitlistRequested;
    private Integer accessGranted;
    private Integer salesContactRequested;
    private String companyName;
    private Integer requestedSeats;
    private String requestNote;
    private LocalDateTime waitlistRequestedAt;
    private LocalDateTime accessGrantedAt;
    private LocalDateTime salesContactRequestedAt;
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

    public String getPlanCodeSnapshot() {
        return planCodeSnapshot;
    }

    public void setPlanCodeSnapshot(String planCodeSnapshot) {
        this.planCodeSnapshot = planCodeSnapshot;
    }

    public Integer getWaitlistRequested() {
        return waitlistRequested;
    }

    public void setWaitlistRequested(Integer waitlistRequested) {
        this.waitlistRequested = waitlistRequested;
    }

    public Integer getAccessGranted() {
        return accessGranted;
    }

    public void setAccessGranted(Integer accessGranted) {
        this.accessGranted = accessGranted;
    }

    public Integer getSalesContactRequested() {
        return salesContactRequested;
    }

    public void setSalesContactRequested(Integer salesContactRequested) {
        this.salesContactRequested = salesContactRequested;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public Integer getRequestedSeats() {
        return requestedSeats;
    }

    public void setRequestedSeats(Integer requestedSeats) {
        this.requestedSeats = requestedSeats;
    }

    public String getRequestNote() {
        return requestNote;
    }

    public void setRequestNote(String requestNote) {
        this.requestNote = requestNote;
    }

    public LocalDateTime getWaitlistRequestedAt() {
        return waitlistRequestedAt;
    }

    public void setWaitlistRequestedAt(LocalDateTime waitlistRequestedAt) {
        this.waitlistRequestedAt = waitlistRequestedAt;
    }

    public LocalDateTime getAccessGrantedAt() {
        return accessGrantedAt;
    }

    public void setAccessGrantedAt(LocalDateTime accessGrantedAt) {
        this.accessGrantedAt = accessGrantedAt;
    }

    public LocalDateTime getSalesContactRequestedAt() {
        return salesContactRequestedAt;
    }

    public void setSalesContactRequestedAt(LocalDateTime salesContactRequestedAt) {
        this.salesContactRequestedAt = salesContactRequestedAt;
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
