package com.mmmail.server.model.vo;

import java.time.LocalDateTime;
import java.util.List;

public record PassMailAliasVo(
        String id,
        String aliasEmail,
        String title,
        String note,
        String forwardToEmail,
        List<String> forwardToEmails,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
