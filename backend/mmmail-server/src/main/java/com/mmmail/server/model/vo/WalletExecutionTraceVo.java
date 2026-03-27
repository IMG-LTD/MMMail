package com.mmmail.server.model.vo;

import java.util.List;

public record WalletExecutionTraceVo(
        String transactionId,
        String currentStatus,
        int integrityScore,
        List<String> warnings,
        List<WalletExecutionTraceStageEventVo> stageEvents
) {
}
