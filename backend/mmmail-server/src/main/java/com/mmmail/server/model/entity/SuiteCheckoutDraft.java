package com.mmmail.server.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("suite_checkout_draft")
public class SuiteCheckoutDraft {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long ownerId;
    private String offerCode;
    private String offerName;
    private String quoteStatus;
    private String checkoutMode;
    private String currencyCode;
    private String billingCycle;
    private Integer seatCount;
    private String organizationName;
    private String domainName;
    private String marketingBadge;
    private String invoiceSummaryJson;
    private String entitlementSummaryJson;
    private String onboardingSummaryJson;
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

    public String getOfferCode() {
        return offerCode;
    }

    public void setOfferCode(String offerCode) {
        this.offerCode = offerCode;
    }

    public String getOfferName() {
        return offerName;
    }

    public void setOfferName(String offerName) {
        this.offerName = offerName;
    }

    public String getQuoteStatus() {
        return quoteStatus;
    }

    public void setQuoteStatus(String quoteStatus) {
        this.quoteStatus = quoteStatus;
    }

    public String getCheckoutMode() {
        return checkoutMode;
    }

    public void setCheckoutMode(String checkoutMode) {
        this.checkoutMode = checkoutMode;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
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

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public String getMarketingBadge() {
        return marketingBadge;
    }

    public void setMarketingBadge(String marketingBadge) {
        this.marketingBadge = marketingBadge;
    }

    public String getInvoiceSummaryJson() {
        return invoiceSummaryJson;
    }

    public void setInvoiceSummaryJson(String invoiceSummaryJson) {
        this.invoiceSummaryJson = invoiceSummaryJson;
    }

    public String getEntitlementSummaryJson() {
        return entitlementSummaryJson;
    }

    public void setEntitlementSummaryJson(String entitlementSummaryJson) {
        this.entitlementSummaryJson = entitlementSummaryJson;
    }

    public String getOnboardingSummaryJson() {
        return onboardingSummaryJson;
    }

    public void setOnboardingSummaryJson(String onboardingSummaryJson) {
        this.onboardingSummaryJson = onboardingSummaryJson;
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
