package com.mmmail.server.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("meet_quality_snapshot")
public class MeetQualitySnapshot {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long roomId;
    private Long ownerId;
    private Long participantId;
    private Integer jitterMs;
    private Integer packetLossPercent;
    private Integer roundTripMs;
    private Integer qualityScore;
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

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public Long getParticipantId() {
        return participantId;
    }

    public void setParticipantId(Long participantId) {
        this.participantId = participantId;
    }

    public Integer getJitterMs() {
        return jitterMs;
    }

    public void setJitterMs(Integer jitterMs) {
        this.jitterMs = jitterMs;
    }

    public Integer getPacketLossPercent() {
        return packetLossPercent;
    }

    public void setPacketLossPercent(Integer packetLossPercent) {
        this.packetLossPercent = packetLossPercent;
    }

    public Integer getRoundTripMs() {
        return roundTripMs;
    }

    public void setRoundTripMs(Integer roundTripMs) {
        this.roundTripMs = roundTripMs;
    }

    public Integer getQualityScore() {
        return qualityScore;
    }

    public void setQualityScore(Integer qualityScore) {
        this.qualityScore = qualityScore;
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
