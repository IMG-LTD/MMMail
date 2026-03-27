package com.mmmail.server.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("suite_billing_subscription_state")
public class SuiteBillingSubscriptionState {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long ownerId;
    private String billingCycle;
    private Integer seatCount;
    private String currencyCode;
    private Integer autoRenew;
    private LocalDateTime currentPeriodEndsAt;
    private Long defaultPaymentMethodId;
    private String pendingActionCode;
    private String pendingOfferCode;
    private String pendingOfferName;
    private String pendingBillingCycle;
    private Integer pendingSeatCount;
    private LocalDateTime pendingEffectiveAt;
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

    public String getBillingCycle() {
        return billingCycle;
    }

    public void setBillingCycle(String billingCycle) {
        this.billingCycle = billingCycle;
    }

    public Integer getSeatCount() {
        return seatCount;
    }

    public void setSeatCount(Integer seatCount) {
        this.seatCount = seatCount;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public Integer getAutoRenew() {
        return autoRenew;
    }

    public void setAutoRenew(Integer autoRenew) {
        this.autoRenew = autoRenew;
    }

    public LocalDateTime getCurrentPeriodEndsAt() {
        return currentPeriodEndsAt;
    }

    public void setCurrentPeriodEndsAt(LocalDateTime currentPeriodEndsAt) {
        this.currentPeriodEndsAt = currentPeriodEndsAt;
    }

    public Long getDefaultPaymentMethodId() {
        return defaultPaymentMethodId;
    }

    public void setDefaultPaymentMethodId(Long defaultPaymentMethodId) {
        this.defaultPaymentMethodId = defaultPaymentMethodId;
    }

    public String getPendingActionCode() {
        return pendingActionCode;
    }

    public void setPendingActionCode(String pendingActionCode) {
        this.pendingActionCode = pendingActionCode;
    }

    public String getPendingOfferCode() {
        return pendingOfferCode;
    }

    public void setPendingOfferCode(String pendingOfferCode) {
        this.pendingOfferCode = pendingOfferCode;
    }

    public String getPendingOfferName() {
        return pendingOfferName;
    }

    public void setPendingOfferName(String pendingOfferName) {
        this.pendingOfferName = pendingOfferName;
    }

    public String getPendingBillingCycle() {
        return pendingBillingCycle;
    }

    public void setPendingBillingCycle(String pendingBillingCycle) {
        this.pendingBillingCycle = pendingBillingCycle;
    }

    public Integer getPendingSeatCount() {
        return pendingSeatCount;
    }

    public void setPendingSeatCount(Integer pendingSeatCount) {
        this.pendingSeatCount = pendingSeatCount;
    }

    public LocalDateTime getPendingEffectiveAt() {
        return pendingEffectiveAt;
    }

    public void setPendingEffectiveAt(LocalDateTime pendingEffectiveAt) {
        this.pendingEffectiveAt = pendingEffectiveAt;
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
