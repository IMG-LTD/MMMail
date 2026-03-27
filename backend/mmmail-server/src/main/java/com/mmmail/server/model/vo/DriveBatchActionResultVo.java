package com.mmmail.server.model.vo;

import java.util.List;

public record DriveBatchActionResultVo(
        int requestedCount,
        int successCount,
        int failedCount,
        List<DriveBatchFailureVo> failedItems
) {
}
