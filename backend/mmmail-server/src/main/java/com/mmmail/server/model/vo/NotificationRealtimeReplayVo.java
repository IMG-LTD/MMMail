package com.mmmail.server.model.vo;

import java.util.List;

public record NotificationRealtimeReplayVo(
        List<NotificationRealtimeEventVo> events,
        long nextCursor
) {
}
