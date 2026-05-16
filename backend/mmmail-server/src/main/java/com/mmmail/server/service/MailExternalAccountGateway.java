package com.mmmail.server.service;

import com.mmmail.server.model.entity.MailExternalAccount;

import java.time.LocalDateTime;
import java.util.List;

public interface MailExternalAccountGateway {

    ConnectionTestResult testConnection(MailExternalAccount account, String secret);

    SyncFetchResult syncInbox(MailExternalAccount account, String secret, Integer maxMessages);

    record ConnectionTestResult(
            boolean imapOk,
            boolean smtpOk,
            long latencyMs,
            String message
    ) {
    }

    record ImportedMessage(
            String uid,
            String fromEmail,
            String subject,
            String body,
            LocalDateTime sentAt
    ) {
    }

    record SyncFetchResult(
            List<ImportedMessage> messages,
            String highWatermark
    ) {
    }
}
