package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record SimpleLoginOverviewVo(
        String orgId,
        long aliasCount,
        long enabledAliasCount,
        long disabledAliasCount,
        long mailboxCount,
        long verifiedMailboxCount,
        String defaultMailboxEmail,
        long reverseAliasContactCount,
        long customDomainCount,
        long verifiedCustomDomainCount,
        String defaultDomain,
        long relayPolicyCount,
        long catchAllDomainCount,
        long subdomainPolicyCount,
        String defaultRelayMailboxEmail,
        LocalDateTime generatedAt
) {
}
