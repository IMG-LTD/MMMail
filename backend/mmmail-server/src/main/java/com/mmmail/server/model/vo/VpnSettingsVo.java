package com.mmmail.server.model.vo;

public record VpnSettingsVo(
        String netshieldMode,
        boolean killSwitchEnabled,
        String defaultConnectionMode,
        String defaultProfileId
) {
}
