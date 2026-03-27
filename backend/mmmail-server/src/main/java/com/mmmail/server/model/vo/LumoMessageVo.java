package com.mmmail.server.model.vo;

import java.time.LocalDateTime;
import java.util.List;

public record LumoMessageVo(
        String messageId,
        String conversationId,
        String role,
        String content,
        int tokenCount,
        LocalDateTime createdAt,
        String capabilityMode,
        String responseLocale,
        boolean webSearchEnabled,
        boolean citationsEnabled,
        List<LumoCitationVo> citations
) {
}
