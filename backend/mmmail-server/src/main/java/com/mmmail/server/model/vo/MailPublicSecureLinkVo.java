package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record MailPublicSecureLinkVo(
        String mailId,
        String subject,
        String senderEmail,
        String recipientEmail,
        String bodyCiphertext,
        String algorithm,
        String passwordHint,
        LocalDateTime expiresAt
) {
}
