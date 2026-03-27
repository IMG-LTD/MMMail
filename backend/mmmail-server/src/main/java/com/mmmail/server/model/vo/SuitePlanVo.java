package com.mmmail.server.model.vo;

import java.util.List;

public record SuitePlanVo(
        String code,
        String name,
        String description,
        String segment,
        String priceMode,
        String priceValue,
        boolean recommended,
        List<String> highlights,
        List<String> upgradeTargets,
        int mailDailySendLimit,
        int contactLimit,
        int calendarEventLimit,
        int calendarShareLimit,
        int driveStorageMb,
        List<String> enabledProducts
) {
}
