package com.mmmail.server.model.vo;

import java.util.List;

public record SuiteBillingCenterVo(
        SuiteBillingSubscriptionSummaryVo subscriptionSummary,
        List<SuiteBillingPaymentMethodVo> paymentMethods,
        List<SuiteBillingInvoiceVo> invoices,
        List<SuiteBillingSubscriptionActionVo> availableActions
) {
}
