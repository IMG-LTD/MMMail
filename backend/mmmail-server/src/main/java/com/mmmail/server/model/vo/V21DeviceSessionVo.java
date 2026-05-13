package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record V21DeviceSessionVo(
        String id,
        String deviceName,
        LocalDateTime lastActiveAt,
        boolean current
) {
}
