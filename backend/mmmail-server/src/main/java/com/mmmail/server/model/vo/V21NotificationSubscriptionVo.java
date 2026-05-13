package com.mmmail.server.model.vo;

public record V21NotificationSubscriptionVo(
        String id,
        String product,
        String channel,
        boolean enabled
) {
}
