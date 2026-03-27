package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record WalletExecutionTraceStageEventVo(
        String stage,
        LocalDateTime at,
        String source,
        String message
) {
}
