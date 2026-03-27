package com.mmmail.server.model.vo;

import java.time.LocalDateTime;
import java.util.List;

public record PassMonitorOverviewVo(
        String scopeType,
        String orgId,
        String currentRole,
        int totalItemCount,
        int trackedItemCount,
        int weakPasswordCount,
        int reusedPasswordCount,
        int inactiveTwoFactorCount,
        int excludedItemCount,
        LocalDateTime generatedAt,
        List<PassMonitorItemVo> weakPasswords,
        List<PassMonitorItemVo> reusedPasswords,
        List<PassMonitorItemVo> inactiveTwoFactorItems,
        List<PassMonitorItemVo> excludedItems
) {
}
