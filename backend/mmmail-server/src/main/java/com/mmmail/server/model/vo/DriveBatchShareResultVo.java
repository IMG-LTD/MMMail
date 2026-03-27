package com.mmmail.server.model.vo;

import java.util.List;

public record DriveBatchShareResultVo(
        int requestedCount,
        int successCount,
        int failedCount,
        List<DriveShareLinkVo> createdShares,
        List<DriveBatchFailureVo> failedItems
) {
}
