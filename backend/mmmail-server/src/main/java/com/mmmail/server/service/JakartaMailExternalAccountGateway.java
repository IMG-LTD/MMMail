package com.mmmail.server.service;

import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.model.entity.MailExternalAccount;
import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.BodyPart;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.Transport;
import jakarta.mail.UIDFolder;
import org.springframework.stereotype.Service;

import java.net.SocketTimeoutException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

@Service
public class JakartaMailExternalAccountGateway implements MailExternalAccountGateway {

    private static final int TIMEOUT_MS = 10_000;

    @Override
    public ConnectionTestResult testConnection(MailExternalAccount account, String secret) {
        long start = System.nanoTime();
        testImap(account, secret);
        testSmtp(account, secret);
        long latencyMs = (System.nanoTime() - start) / 1_000_000;
        return new ConnectionTestResult(true, true, latencyMs, "ok");
    }

    @Override
    public SyncFetchResult syncInbox(MailExternalAccount account, String secret, Integer maxMessages) {
        try (Store store = openStore(account, secret)) {
            Folder folder = store.getFolder("INBOX");
            folder.open(Folder.READ_ONLY);
            try {
                return readMessages(account, folder, maxMessages == null ? 100 : maxMessages);
            } finally {
                folder.close(false);
            }
        } catch (MessagingException ex) {
            throw translate(ex);
        }
    }

    private void testImap(MailExternalAccount account, String secret) {
        try (Store ignored = openStore(account, secret)) {
            // Connection success is enough for the contract-level test endpoint.
        } catch (MessagingException ex) {
            throw translate(ex);
        }
    }

    private void testSmtp(MailExternalAccount account, String secret) {
        try (Transport transport = smtpSession(account).getTransport(smtpProtocol(account))) {
            transport.connect(account.getSmtpHost(), account.getSmtpPort(), account.getUsername(), secret);
        } catch (MessagingException ex) {
            throw translate(ex);
        }
    }

    private Store openStore(MailExternalAccount account, String secret) throws MessagingException {
        Store store = imapSession(account).getStore(imapProtocol(account));
        store.connect(account.getImapHost(), account.getImapPort(), account.getUsername(), secret);
        return store;
    }

    private SyncFetchResult readMessages(MailExternalAccount account, Folder folder, int maxMessages) throws MessagingException {
        Message[] messages = folder.getMessages();
        int start = Math.max(1, messages.length - Math.max(1, maxMessages) + 1);
        List<ImportedMessage> imported = new ArrayList<>();
        String highWatermark = account.getUidHighWatermark();
        for (int sequence = start; sequence <= messages.length; sequence++) {
            Message message = messages[sequence - 1];
            String uid = uid(folder, message, sequence);
            if (alreadySynced(account.getUidHighWatermark(), uid)) {
                continue;
            }
            imported.add(toImportedMessage(message, uid));
            highWatermark = uid;
        }
        return new SyncFetchResult(imported, highWatermark);
    }

    private ImportedMessage toImportedMessage(Message message, String uid) throws MessagingException {
        return new ImportedMessage(
                uid,
                firstAddress(message),
                message.getSubject(),
                readBody(message),
                toLocalDateTime(message.getReceivedDate())
        );
    }

    private String uid(Folder folder, Message message, int sequence) throws MessagingException {
        if (folder instanceof UIDFolder uidFolder) {
            long uid = uidFolder.getUID(message);
            return uid > 0 ? String.valueOf(uid) : String.valueOf(sequence);
        }
        return String.valueOf(sequence);
    }

    private boolean alreadySynced(String highWatermark, String uid) {
        try {
            return highWatermark != null && Long.parseLong(uid) <= Long.parseLong(highWatermark);
        } catch (NumberFormatException ex) {
            return highWatermark != null && highWatermark.compareTo(uid) >= 0;
        }
    }

    private String firstAddress(Message message) throws MessagingException {
        if (message.getFrom() == null || message.getFrom().length == 0) {
            return "";
        }
        return message.getFrom()[0].toString();
    }

    private String readBody(Part part) throws MessagingException {
        try {
            if (part.isMimeType("text/plain")) {
                return String.valueOf(part.getContent());
            }
            if (part.isMimeType("text/html")) {
                return String.valueOf(part.getContent());
            }
            if (part.getContent() instanceof Multipart multipart) {
                return readMultipart(multipart);
            }
            return "";
        } catch (Exception ex) {
            throw new BizException(ErrorCode.MAIL_EXTERNAL_ACCOUNT_CONFIG, "External mail body cannot be parsed");
        }
    }

    private String readMultipart(Multipart multipart) throws MessagingException {
        for (int index = 0; index < multipart.getCount(); index++) {
            BodyPart bodyPart = multipart.getBodyPart(index);
            String body = readBody(bodyPart);
            if (!body.isBlank()) {
                return body;
            }
        }
        return "";
    }

    private Session imapSession(MailExternalAccount account) {
        Properties properties = baseProperties();
        String protocol = imapProtocol(account);
        properties.put("mail.store.protocol", protocol);
        properties.put("mail." + protocol + ".ssl.enable", String.valueOf(account.getImapSsl() == 1));
        applyOauth(account, properties, protocol);
        return Session.getInstance(properties);
    }

    private Session smtpSession(MailExternalAccount account) {
        Properties properties = baseProperties();
        String protocol = smtpProtocol(account);
        properties.put("mail.transport.protocol", protocol);
        properties.put("mail." + protocol + ".auth", "true");
        properties.put("mail." + protocol + ".ssl.enable", String.valueOf(account.getSmtpSsl() == 1));
        properties.put("mail." + protocol + ".starttls.enable", String.valueOf(account.getSmtpStarttls() == 1));
        applyOauth(account, properties, protocol);
        return Session.getInstance(properties);
    }

    private Properties baseProperties() {
        Properties properties = new Properties();
        properties.put("mail.imap.connectiontimeout", String.valueOf(TIMEOUT_MS));
        properties.put("mail.imap.timeout", String.valueOf(TIMEOUT_MS));
        properties.put("mail.imaps.connectiontimeout", String.valueOf(TIMEOUT_MS));
        properties.put("mail.imaps.timeout", String.valueOf(TIMEOUT_MS));
        properties.put("mail.smtp.connectiontimeout", String.valueOf(TIMEOUT_MS));
        properties.put("mail.smtp.timeout", String.valueOf(TIMEOUT_MS));
        properties.put("mail.smtps.connectiontimeout", String.valueOf(TIMEOUT_MS));
        properties.put("mail.smtps.timeout", String.valueOf(TIMEOUT_MS));
        return properties;
    }

    private void applyOauth(MailExternalAccount account, Properties properties, String protocol) {
        if ("OAUTH2".equals(account.getAuthMode())) {
            properties.put("mail." + protocol + ".auth.mechanisms", "XOAUTH2");
        }
    }

    private String imapProtocol(MailExternalAccount account) {
        return account.getImapSsl() == 1 ? "imaps" : "imap";
    }

    private String smtpProtocol(MailExternalAccount account) {
        return account.getSmtpSsl() == 1 ? "smtps" : "smtp";
    }

    private LocalDateTime toLocalDateTime(Date date) {
        Date safeDate = date == null ? new Date() : date;
        return LocalDateTime.ofInstant(safeDate.toInstant(), ZoneId.systemDefault());
    }

    private BizException translate(MessagingException ex) {
        if (ex instanceof AuthenticationFailedException) {
            return new BizException(ErrorCode.MAIL_EXTERNAL_AUTH_INVALID, "External mail authentication failed");
        }
        if (hasTimeout(ex)) {
            return new BizException(ErrorCode.MAIL_EXTERNAL_TIMEOUT, "External mail server timed out");
        }
        return new BizException(ErrorCode.MAIL_EXTERNAL_ACCOUNT_CONFIG, ex.getMessage());
    }

    private boolean hasTimeout(Throwable error) {
        Throwable cursor = error;
        while (cursor != null) {
            if (cursor instanceof SocketTimeoutException) {
                return true;
            }
            cursor = cursor.getCause();
        }
        return false;
    }
}
