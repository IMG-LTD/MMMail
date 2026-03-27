package com.mmmail.server.model.vo;

public record UserPreferenceVo(
        String displayName,
        String signature,
        String timezone,
        String preferredLocale,
        String mailAddressMode,
        int autoSaveSeconds,
        int undoSendSeconds,
        int driveVersionRetentionCount,
        int driveVersionRetentionDays
) {
}
