package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record SimpleLoginRelayPolicyVo(
        String id,
        String orgId,
        String customDomainId,
        String domain,
        boolean catchAllEnabled,
        String subdomainMode,
        String defaultMailboxId,
        String defaultMailboxEmail,
        String note,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
