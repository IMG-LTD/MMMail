package com.mmmail.server.model.vo;

public record RuleResolutionVo(
        String senderEmail,
        String senderDomain,
        boolean trustedSender,
        boolean blockedSender,
        boolean trustedDomain,
        boolean blockedDomain,
        String effectiveFolder,
        String reason,
        String matchedRule
) {
}
