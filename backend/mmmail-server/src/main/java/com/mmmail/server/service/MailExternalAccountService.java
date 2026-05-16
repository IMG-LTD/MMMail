package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.mapper.MailExternalAccountMapper;
import com.mmmail.server.mapper.MailMessageMapper;
import com.mmmail.server.model.dto.CreateMailExternalAccountRequest;
import com.mmmail.server.model.dto.MailExternalServerRequest;
import com.mmmail.server.model.dto.UpdateMailExternalAccountRequest;
import com.mmmail.server.model.entity.MailExternalAccount;
import com.mmmail.server.model.entity.MailMessage;
import com.mmmail.server.model.vo.MailExternalAccountSyncVo;
import com.mmmail.server.model.vo.MailExternalAccountTestVo;
import com.mmmail.server.model.vo.MailExternalAccountVo;
import com.mmmail.server.model.vo.MailExternalServerVo;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class MailExternalAccountService {

    private static final int MAX_SYNC_MESSAGES = 100;
    private static final int MAX_ERROR_LENGTH = 512;

    private final MailExternalAccountMapper accountMapper;
    private final MailMessageMapper mailMessageMapper;
    private final MailExternalAccountSecretCodec secretCodec;
    private final MailExternalAccountGateway gateway;

    public MailExternalAccountService(
            MailExternalAccountMapper accountMapper,
            MailMessageMapper mailMessageMapper,
            MailExternalAccountSecretCodec secretCodec,
            MailExternalAccountGateway gateway
    ) {
        this.accountMapper = accountMapper;
        this.mailMessageMapper = mailMessageMapper;
        this.secretCodec = secretCodec;
        this.gateway = gateway;
    }

    public List<MailExternalAccountVo> list(Long userId) {
        return accountMapper.selectList(new LambdaQueryWrapper<MailExternalAccount>()
                        .eq(MailExternalAccount::getOwnerId, userId)
                        .orderByDesc(MailExternalAccount::getUpdatedAt))
                .stream()
                .map(this::toVo)
                .toList();
    }

    @Transactional
    public MailExternalAccountVo create(Long userId, CreateMailExternalAccountRequest request) {
        MailExternalAccount account = new MailExternalAccount();
        LocalDateTime now = LocalDateTime.now();
        account.setOwnerId(userId);
        applyCreate(account, request);
        account.setSyncStatus("INITIAL_SYNC");
        account.setCreatedAt(now);
        account.setUpdatedAt(now);
        account.setDeleted(0);
        try {
            accountMapper.insert(account);
        } catch (DuplicateKeyException ex) {
            throw new BizException(ErrorCode.MAIL_EXTERNAL_ACCOUNT_CONFIG, "External mail account already exists");
        }
        return toVo(account);
    }

    public MailExternalAccountVo get(Long userId, Long accountId) {
        return toVo(requireAccount(userId, accountId));
    }

    @Transactional
    public MailExternalAccountVo update(Long userId, Long accountId, UpdateMailExternalAccountRequest request) {
        MailExternalAccount account = requireAccount(userId, accountId);
        applyUpdate(account, request);
        account.setUpdatedAt(LocalDateTime.now());
        try {
            accountMapper.updateById(account);
        } catch (DuplicateKeyException ex) {
            throw new BizException(ErrorCode.MAIL_EXTERNAL_ACCOUNT_CONFIG, "External mail account already exists");
        }
        return toVo(account);
    }

    @Transactional
    public void delete(Long userId, Long accountId) {
        MailExternalAccount account = requireAccount(userId, accountId);
        accountMapper.deleteById(account.getId());
    }

    @Transactional
    public MailExternalAccountTestVo test(Long userId, Long accountId) {
        MailExternalAccount account = requireAccount(userId, accountId);
        MailExternalAccountGateway.ConnectionTestResult result = gateway.testConnection(account, decrypt(account));
        account.setLastError(null);
        account.setUpdatedAt(LocalDateTime.now());
        accountMapper.updateById(account);
        return new MailExternalAccountTestVo(result.imapOk(), result.smtpOk(), result.latencyMs(), result.message());
    }

    @Transactional
    public MailExternalAccountSyncVo sync(Long userId, Long accountId) {
        MailExternalAccount account = requireAccount(userId, accountId);
        try {
            MailExternalAccountGateway.SyncFetchResult result = gateway.syncInbox(account, decrypt(account), MAX_SYNC_MESSAGES);
            int imported = importMessages(userId, account, result.messages());
            updateSyncSuccess(account, result.highWatermark());
            return new MailExternalAccountSyncVo(String.valueOf(account.getId()), account.getSyncStatus(), imported,
                    result.messages().size() - imported, account.getLastSyncAt(), account.getLastError());
        } catch (BizException ex) {
            updateSyncError(account, ex.getMessage());
            throw ex;
        }
    }

    private void applyCreate(MailExternalAccount account, CreateMailExternalAccountRequest request) {
        account.setProvider(normalizeToken(request.provider()));
        account.setAuthMode(normalizeAuthMode(request.authMode()));
        account.setEmail(normalizeEmail(request.email()));
        account.setUsername(requireText(request.username(), "External mail username is required"));
        account.setSecretCiphertext(secretCodec.encrypt(requireSecret(request.authMode(), request.password(), request.oauthRefreshToken())));
        applyServer(account, request.imap(), request.smtp());
    }

    private void applyUpdate(MailExternalAccount account, UpdateMailExternalAccountRequest request) {
        boolean secretRequired = false;
        if (StringUtils.hasText(request.provider())) {
            account.setProvider(normalizeToken(request.provider()));
        }
        if (StringUtils.hasText(request.authMode())) {
            String authMode = normalizeAuthMode(request.authMode());
            secretRequired = !authMode.equals(account.getAuthMode());
            account.setAuthMode(authMode);
        }
        if (StringUtils.hasText(request.email())) {
            account.setEmail(normalizeEmail(request.email()));
        }
        if (StringUtils.hasText(request.username())) {
            account.setUsername(requireText(request.username(), "External mail username is required"));
        }
        applySecretUpdate(account, request, secretRequired);
        applyServerUpdate(account, request.imap(), request.smtp());
    }

    private void applySecretUpdate(MailExternalAccount account, UpdateMailExternalAccountRequest request, boolean required) {
        String secret = secretForMode(account.getAuthMode(), request.password(), request.oauthRefreshToken());
        if (StringUtils.hasText(secret)) {
            account.setSecretCiphertext(secretCodec.encrypt(secret));
            return;
        }
        if (required) {
            throw new BizException(ErrorCode.MAIL_EXTERNAL_ACCOUNT_CONFIG, "External account secret is required");
        }
    }

    private void applyServer(MailExternalAccount account, MailExternalServerRequest imap, MailExternalServerRequest smtp) {
        account.setImapHost(requireText(imap.host(), "IMAP host is required"));
        account.setImapPort(imap.port());
        account.setImapSsl(flag(imap.ssl(), true));
        account.setSmtpHost(requireText(smtp.host(), "SMTP host is required"));
        account.setSmtpPort(smtp.port());
        account.setSmtpStarttls(flag(smtp.starttls(), true));
        account.setSmtpSsl(flag(smtp.ssl(), false));
    }

    private void applyServerUpdate(MailExternalAccount account, MailExternalServerRequest imap, MailExternalServerRequest smtp) {
        if (imap != null) {
            account.setImapHost(requireText(imap.host(), "IMAP host is required"));
            account.setImapPort(imap.port());
            account.setImapSsl(flag(imap.ssl(), true));
        }
        if (smtp != null) {
            account.setSmtpHost(requireText(smtp.host(), "SMTP host is required"));
            account.setSmtpPort(smtp.port());
            account.setSmtpStarttls(flag(smtp.starttls(), true));
            account.setSmtpSsl(flag(smtp.ssl(), false));
        }
    }

    private int importMessages(Long userId, MailExternalAccount account, List<MailExternalAccountGateway.ImportedMessage> messages) {
        int imported = 0;
        for (MailExternalAccountGateway.ImportedMessage message : messages) {
            if (insertImportedMessage(userId, account, message)) {
                imported++;
            }
        }
        return imported;
    }

    private boolean insertImportedMessage(Long userId, MailExternalAccount account, MailExternalAccountGateway.ImportedMessage message) {
        if (existsImportedMessage(userId, account, message.uid())) {
            return false;
        }
        MailMessage mail = new MailMessage();
        LocalDateTime now = LocalDateTime.now();
        mail.setOwnerId(userId);
        mail.setPeerEmail(message.fromEmail());
        mail.setSenderEmail(message.fromEmail());
        mail.setDirection("IN");
        mail.setFolderType("INBOX");
        mail.setSubject(defaultText(message.subject(), "(no subject)"));
        mail.setBodyCiphertext(defaultText(message.body(), ""));
        mail.setBodyE2eeEnabled(0);
        mail.setIsRead(0);
        mail.setIsStarred(0);
        mail.setIsDraft(0);
        mail.setLabelsJson("[]");
        mail.setIdempotencyKey(idempotencyKey(account, message.uid()));
        mail.setSentAt(message.sentAt() == null ? now : message.sentAt());
        mail.setCreatedAt(now);
        mail.setUpdatedAt(now);
        mail.setDeleted(0);
        mailMessageMapper.insert(mail);
        return true;
    }

    private boolean existsImportedMessage(Long userId, MailExternalAccount account, String uid) {
        Long count = mailMessageMapper.selectCount(new LambdaQueryWrapper<MailMessage>()
                .eq(MailMessage::getOwnerId, userId)
                .eq(MailMessage::getIdempotencyKey, idempotencyKey(account, uid)));
        return count != null && count > 0;
    }

    private void updateSyncSuccess(MailExternalAccount account, String highWatermark) {
        LocalDateTime now = LocalDateTime.now();
        account.setUidHighWatermark(highWatermark);
        account.setSyncStatus("SYNCED");
        account.setLastSyncAt(now);
        account.setLastError(null);
        account.setUpdatedAt(now);
        accountMapper.updateById(account);
    }

    private void updateSyncError(MailExternalAccount account, String message) {
        account.setSyncStatus("ERROR");
        account.setLastError(truncate(message));
        account.setUpdatedAt(LocalDateTime.now());
        accountMapper.updateById(account);
    }

    private MailExternalAccount requireAccount(Long userId, Long accountId) {
        MailExternalAccount account = accountMapper.selectOne(new LambdaQueryWrapper<MailExternalAccount>()
                .eq(MailExternalAccount::getOwnerId, userId)
                .eq(MailExternalAccount::getId, accountId));
        if (account == null) {
            throw new BizException(ErrorCode.MAIL_EXTERNAL_ACCOUNT_NOT_FOUND);
        }
        return account;
    }

    private MailExternalAccountVo toVo(MailExternalAccount account) {
        return new MailExternalAccountVo(
                String.valueOf(account.getId()),
                account.getProvider(),
                account.getAuthMode(),
                account.getEmail(),
                account.getUsername(),
                new MailExternalServerVo(account.getImapHost(), account.getImapPort(), account.getImapSsl() == 1, false),
                new MailExternalServerVo(account.getSmtpHost(), account.getSmtpPort(), account.getSmtpSsl() == 1, account.getSmtpStarttls() == 1),
                account.getSyncStatus(),
                account.getLastSyncAt(),
                account.getLastError(),
                account.getCreatedAt(),
                account.getUpdatedAt()
        );
    }

    private String requireSecret(String authMode, String password, String oauthRefreshToken) {
        String secret = secretForMode(normalizeAuthMode(authMode), password, oauthRefreshToken);
        if (!StringUtils.hasText(secret)) {
            throw new BizException(ErrorCode.MAIL_EXTERNAL_ACCOUNT_CONFIG, "External account secret is required");
        }
        return secret;
    }

    private String secretForMode(String authMode, String password, String oauthRefreshToken) {
        if ("OAUTH2".equals(normalizeAuthMode(authMode))) {
            return oauthRefreshToken;
        }
        return password;
    }

    private String decrypt(MailExternalAccount account) {
        return secretCodec.decrypt(account.getSecretCiphertext());
    }

    private String normalizeToken(String value) {
        return requireText(value, "External mail field is required").toUpperCase(Locale.ROOT);
    }

    private String normalizeAuthMode(String value) {
        String normalized = normalizeToken(value);
        if (!List.of("PASSWORD", "OAUTH2").contains(normalized)) {
            throw new BizException(ErrorCode.MAIL_EXTERNAL_ACCOUNT_CONFIG, "External mail auth mode is unsupported");
        }
        return normalized;
    }

    private String normalizeEmail(String value) {
        return requireText(value, "External mail email is required").toLowerCase(Locale.ROOT);
    }

    private String requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BizException(ErrorCode.MAIL_EXTERNAL_ACCOUNT_CONFIG, message);
        }
        return value.trim();
    }

    private int flag(Boolean value, boolean defaultValue) {
        return (value == null ? defaultValue : value) ? 1 : 0;
    }

    private String defaultText(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private String idempotencyKey(MailExternalAccount account, String uid) {
        return "external-" + account.getId() + "-" + defaultText(uid, "unknown");
    }

    private String truncate(String value) {
        if (value == null || value.length() <= MAX_ERROR_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_ERROR_LENGTH);
    }
}
