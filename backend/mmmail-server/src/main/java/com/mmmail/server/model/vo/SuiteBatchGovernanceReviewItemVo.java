package com.mmmail.server.model.vo;

public record SuiteBatchGovernanceReviewItemVo(
        String requestId,
        boolean success,
        Integer errorCode,
        String message,
        SuiteGovernanceChangeRequestVo result
) {
}
