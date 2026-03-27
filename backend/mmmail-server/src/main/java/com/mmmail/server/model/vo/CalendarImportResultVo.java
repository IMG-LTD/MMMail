package com.mmmail.server.model.vo;

import java.util.List;

public record CalendarImportResultVo(
        int totalCount,
        int importedCount,
        List<String> eventIds
) {
}
