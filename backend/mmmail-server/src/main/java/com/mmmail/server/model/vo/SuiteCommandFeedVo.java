package com.mmmail.server.model.vo;

import java.time.LocalDateTime;
import java.util.List;

public record SuiteCommandFeedVo(
        LocalDateTime generatedAt,
        int limit,
        int total,
        List<SuiteCommandFeedItemVo> items
) {
}
