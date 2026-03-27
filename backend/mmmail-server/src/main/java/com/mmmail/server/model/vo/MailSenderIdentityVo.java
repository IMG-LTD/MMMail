package com.mmmail.server.model.vo;

public record MailSenderIdentityVo(
        String identityId,
        String orgId,
        String orgName,
        String memberId,
        String emailAddress,
        String displayName,
        String source,
        String status,
        boolean defaultIdentity
) {
}
