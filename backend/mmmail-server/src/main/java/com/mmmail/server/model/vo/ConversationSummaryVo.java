package com.mmmail.server.model.vo;

import java.time.LocalDateTime;
import java.util.List;

public record ConversationSummaryVo(
        String conversationId,
        String subject,
        List<String> participants,
        long messageCount,
        long unreadCount,
        LocalDateTime latestAt
) {
}
