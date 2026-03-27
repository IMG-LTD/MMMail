package com.mmmail.server.model.vo;

import java.time.LocalDateTime;
import java.util.List;

public record SuiteCollaborationSyncVo(
        String kind,
        LocalDateTime generatedAt,
        long syncCursor,
        String syncVersion,
        boolean hasUpdates,
        int total,
        List<SuiteCollaborationEventVo> items
) {
}
