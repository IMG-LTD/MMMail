package com.mmmail.server.model.vo;

public record V21SecuritySettingsVo(
        boolean mfaEnabled,
        String recoveryEmail
) {
}
