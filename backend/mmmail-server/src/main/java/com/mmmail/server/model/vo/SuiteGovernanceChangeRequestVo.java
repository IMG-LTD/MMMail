package com.mmmail.server.model.vo;

import java.time.LocalDateTime;
import java.util.List;

public record SuiteGovernanceChangeRequestVo(
        String requestId,
        String orgId,
        String ownerId,
        String templateCode,
        String templateName,
        String status,
        String reason,
        boolean requireDualReview,
        String reviewStage,
        String firstReviewNote,
        LocalDateTime firstReviewedAt,
        Long firstReviewedByUserId,
        Long firstReviewedBySessionId,
        Long secondReviewerUserId,
        String reviewNote,
        String approvalNote,
        String rollbackReason,
        LocalDateTime requestedAt,
        LocalDateTime reviewDueAt,
        boolean reviewSlaBreached,
        LocalDateTime reviewedAt,
        Long reviewedByUserId,
        Long reviewedBySessionId,
        LocalDateTime approvedAt,
        LocalDateTime executedAt,
        Long executedByUserId,
        Long executedBySessionId,
        LocalDateTime rolledBackAt,
        List<String> actionCodes,
        List<String> rollbackActionCodes,
        List<SuiteRemediationExecutionResultVo> executionResults,
        List<SuiteRemediationExecutionResultVo> rollbackResults
) {
}
