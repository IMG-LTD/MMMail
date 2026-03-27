package com.mmmail.server.model.vo;

import java.time.LocalDateTime;
import java.util.List;

public record SuiteBatchGovernanceReviewResultVo(
        LocalDateTime generatedAt,
        String decision,
        int totalCount,
        int successCount,
        int failedCount,
        List<SuiteBatchGovernanceReviewItemVo> items
) {
}
