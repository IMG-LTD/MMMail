package com.mmmail.server.model.vo;

import java.util.List;

public record CalendarSettingsVo(
        String defaultTimezone,
        String weekStartsOn,
        List<String> workingHours
) {
}
