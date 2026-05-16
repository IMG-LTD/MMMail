package com.mmmail.server.model.vo;

public record WebPushTestDeliveryVo(
        String deliveryId,
        int attempted,
        int delivered
) {
}
