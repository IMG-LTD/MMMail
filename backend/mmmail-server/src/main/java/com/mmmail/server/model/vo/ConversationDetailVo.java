package com.mmmail.server.model.vo;

import java.util.List;

public record ConversationDetailVo(
        String conversationId,
        String subject,
        List<MailSummaryVo> messages
) {
}
