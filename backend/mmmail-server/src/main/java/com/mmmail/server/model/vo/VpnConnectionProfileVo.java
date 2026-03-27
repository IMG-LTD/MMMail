package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record VpnConnectionProfileVo(
        String profileId,
        String name,
        String protocol,
        String routingMode,
        String targetServerId,
        String targetCountry,
        boolean secureCoreEnabled,
        String netshieldMode,
        boolean killSwitchEnabled,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
