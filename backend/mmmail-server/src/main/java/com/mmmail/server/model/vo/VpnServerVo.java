package com.mmmail.server.model.vo;

public record VpnServerVo(
        String serverId,
        String country,
        String city,
        String tier,
        String status,
        int loadPercent
) {
}
