package com.mmmail.server.model.vo;

public record SuiteWebPushStatusVo(
        boolean enabled,
        String deliveryScope,
        String vapidPublicKey,
        String message,
        int activeSubscriptionCount
) {
}
