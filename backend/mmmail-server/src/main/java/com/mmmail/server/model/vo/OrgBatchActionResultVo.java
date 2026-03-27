package com.mmmail.server.model.vo;

import java.util.List;

public record OrgBatchActionResultVo(
        int requestedCount,
        List<String> successIds,
        List<OrgBatchFailureVo> failedItems
) {
}
