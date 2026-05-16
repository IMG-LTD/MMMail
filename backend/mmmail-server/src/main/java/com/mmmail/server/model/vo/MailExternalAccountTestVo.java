package com.mmmail.server.model.vo;

public record MailExternalAccountTestVo(
        boolean imapOk,
        boolean smtpOk,
        long latencyMs,
        String message
) {
}
