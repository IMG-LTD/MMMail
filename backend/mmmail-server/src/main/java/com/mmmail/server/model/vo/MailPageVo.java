package com.mmmail.server.model.vo;

import java.util.List;

public record MailPageVo(
        List<MailSummaryVo> items,
        long total,
        long page,
        long size,
        long unread
) {
}
