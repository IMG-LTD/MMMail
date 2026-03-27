package com.mmmail.server.model.vo;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record SuiteCollaborationCenterVo(
        LocalDateTime generatedAt,
        int limit,
        int total,
        Map<String, Integer> productCounts,
        long syncCursor,
        String syncVersion,
        List<SuiteCollaborationEventVo> items
) {
}
