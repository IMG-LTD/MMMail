package com.mmmail.server.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("vpn_connection_profile")
public class VpnConnectionProfile {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long ownerId;
    private String name;
    private String protocol;
    private String routingMode;
    private String targetServerId;
    private String targetCountry;
    private Integer secureCoreEnabled;
    private String netshieldMode;
    private Integer killSwitchEnabled;
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

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getRoutingMode() {
        return routingMode;
    }

    public void setRoutingMode(String routingMode) {
        this.routingMode = routingMode;
    }

    public String getTargetServerId() {
        return targetServerId;
    }

    public void setTargetServerId(String targetServerId) {
        this.targetServerId = targetServerId;
    }

    public String getTargetCountry() {
        return targetCountry;
    }

    public void setTargetCountry(String targetCountry) {
        this.targetCountry = targetCountry;
    }

    public Integer getSecureCoreEnabled() {
        return secureCoreEnabled;
    }

    public void setSecureCoreEnabled(Integer secureCoreEnabled) {
        this.secureCoreEnabled = secureCoreEnabled;
    }

    public String getNetshieldMode() {
        return netshieldMode;
    }

    public void setNetshieldMode(String netshieldMode) {
        this.netshieldMode = netshieldMode;
    }

    public Integer getKillSwitchEnabled() {
        return killSwitchEnabled;
    }

    public void setKillSwitchEnabled(Integer killSwitchEnabled) {
        this.killSwitchEnabled = killSwitchEnabled;
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
