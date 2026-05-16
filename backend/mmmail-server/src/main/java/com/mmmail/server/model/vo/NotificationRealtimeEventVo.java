package com.mmmail.server.model.vo;

public record NotificationRealtimeEventVo(
        String type,
        String channel,
        long seq,
        NotificationRealtimePayloadVo payload
) {
}
