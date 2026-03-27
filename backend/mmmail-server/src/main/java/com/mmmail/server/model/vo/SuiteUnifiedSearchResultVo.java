package com.mmmail.server.model.vo;

import java.time.LocalDateTime;
import java.util.List;

public record SuiteUnifiedSearchResultVo(
        LocalDateTime generatedAt,
        String keyword,
        int limit,
        int total,
        List<SuiteUnifiedSearchItemVo> items
) {
}
