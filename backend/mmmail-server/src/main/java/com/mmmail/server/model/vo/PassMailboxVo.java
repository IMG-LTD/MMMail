package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record PassMailboxVo(
        String id,
        String mailboxEmail,
        String status,
        boolean defaultMailbox,
        boolean primaryMailbox,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime verifiedAt
) {
}
