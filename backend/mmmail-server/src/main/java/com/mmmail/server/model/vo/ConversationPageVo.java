package com.mmmail.server.model.vo;

import java.util.List;

public record ConversationPageVo(
        List<ConversationSummaryVo> items,
        long total,
        long page,
        long size
) {
}
