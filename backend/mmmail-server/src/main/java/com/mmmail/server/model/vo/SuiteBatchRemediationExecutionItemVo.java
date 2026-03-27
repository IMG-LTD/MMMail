package com.mmmail.server.model.vo;

public record SuiteBatchRemediationExecutionItemVo(
        String actionCode,
        boolean success,
        Integer errorCode,
        String message,
        SuiteRemediationExecutionResultVo executionResult
) {
}
