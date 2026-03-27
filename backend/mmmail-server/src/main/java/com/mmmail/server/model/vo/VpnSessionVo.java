package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record VpnSessionVo(
        String sessionId,
        String serverId,
        String serverCountry,
        String serverCity,
        String serverTier,
        String protocol,
        String status,
        String profileId,
        String profileName,
        String netshieldMode,
        boolean killSwitchEnabled,
        String connectionSource,
        LocalDateTime connectedAt,
        LocalDateTime disconnectedAt,
        long durationSeconds
) {
}
