package com.mmmail.server.model.vo;

import java.time.LocalDateTime;
import java.util.List;

public record SuiteBatchRemediationExecutionResultVo(
        LocalDateTime generatedAt,
        int totalCount,
        int successCount,
        int failedCount,
        List<SuiteBatchRemediationExecutionItemVo> items
) {
}
