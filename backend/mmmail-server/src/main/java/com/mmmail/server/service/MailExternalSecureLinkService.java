package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.foundation.security.PublicShareTokenCodec;
import com.mmmail.server.mapper.MailExternalSecureLinkMapper;
import com.mmmail.server.mapper.MailMessageMapper;
import com.mmmail.server.model.dto.MailExternalAccessRequest;
import com.mmmail.server.model.entity.MailExternalSecureLink;
import com.mmmail.server.model.entity.MailMessage;
import com.mmmail.server.model.vo.MailPublicSecureLinkVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class MailExternalSecureLinkService {

    public static final String PASSWORD_PROTECTED_MODE = "PASSWORD_PROTECTED";

    private static final int DEFAULT_EXPIRE_DAYS = 7;
    private static final int MAX_EXPIRE_DAYS = 30;
    private static final DateTimeFormatter NOTIFICATION_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final PublicShareTokenCodec publicShareTokenCodec = new PublicShareTokenCodec();
    private final MailExternalSecureLinkMapper mailExternalSecureLinkMapper;
    private final MailMessageMapper mailMessageMapper;
    private final MailAttachmentService mailAttachmentService;
    private final AuditService auditService;

    public MailExternalSecureLinkService(
            MailExternalSecureLinkMapper mailExternalSecureLinkMapper,
            MailMessageMapper mailMessageMapper,
            MailAttachmentService mailAttachmentService,
            AuditService auditService
    ) {
        this.mailExternalSecureLinkMapper = mailExternalSecureLinkMapper;
        this.mailMessageMapper = mailMessageMapper;
        this.mailAttachmentService = mailAttachmentService;
        this.auditService = auditService;
    }

    public boolean isPasswordProtected(MailExternalAccessRequest accessRequest) {
        return accessRequest != null && PASSWORD_PROTECTED_MODE.equals(normalizeMode(accessRequest.mode()));
    }

    @Transactional
    public MailExternalSecureLink createSecureLink(
            Long ownerId,
            MailMessage mail,
            String recipientEmail,
            MailExternalAccessRequest accessRequest,
            LocalDateTime deliveryAt,
            String publicBaseUrl
    ) {
        if (mail.getId() == null) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "Mail must be persisted before creating secure link");
        }
        if (!isPasswordProtected(accessRequest)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Unsupported external Mail E2EE access mode");
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = validateExpiresAt(accessRequest.expiresAt(), deliveryAt);
        MailExternalSecureLink existing = findByMailId(mail.getId());
        if (existing != null) {
            existing.setRecipientEmail(requireRecipientEmail(recipientEmail));
            existing.setTokenHash(publicShareTokenCodec.hash(existing.getToken()));
            existing.setPublicUrl(buildPublicUrl(publicBaseUrl, existing.getToken()));
            existing.setPasswordHint(normalizePasswordHint(accessRequest.passwordHint()));
            existing.setExpiresAt(expiresAt);
            existing.setRevokedAt(null);
            existing.setUpdatedAt(now);
            mailExternalSecureLinkMapper.updateById(existing);
            return existing;
        }
        MailExternalSecureLink link = new MailExternalSecureLink();
        link.setMailId(mail.getId());
        link.setOwnerId(ownerId);
        link.setRecipientEmail(requireRecipientEmail(recipientEmail));
        String rawToken = publicShareTokenCodec.generateRawToken();
        link.setToken(rawToken);
        link.setTokenHash(publicShareTokenCodec.hash(rawToken));
        link.setPublicUrl(buildPublicUrl(publicBaseUrl, link.getToken()));
        link.setPasswordHint(normalizePasswordHint(accessRequest.passwordHint()));
        link.setExpiresAt(expiresAt);
        link.setRevokedAt(null);
        link.setLastAccessedAt(null);
        link.setCreatedAt(now);
        link.setUpdatedAt(now);
        link.setDeleted(0);
        mailExternalSecureLinkMapper.insert(link);
        return link;
    }

    public MailExternalSecureLink findActiveByMailId(Long mailId) {
        MailExternalSecureLink link = findByMailId(mailId);
        if (link == null) {
            return null;
        }
        validateLinkActive(link);
        return link;
    }

    @Transactional
    public MailPublicSecureLinkVo getPublicSecureLink(String token, String ipAddress) {
        MailExternalSecureLink link = requireActiveLink(token);
        MailMessage mail = requireEncryptedMail(link.getMailId());
        LocalDateTime now = LocalDateTime.now();
        link.setLastAccessedAt(now);
        link.setUpdatedAt(now);
        mailExternalSecureLinkMapper.updateById(link);
        auditService.record(
                null,
                "MAIL_SECURE_LINK_VIEW",
                "mailId=" + link.getMailId() + ",recipient=" + link.getRecipientEmail(),
                ipAddress
        );
        return new MailPublicSecureLinkVo(
                String.valueOf(mail.getId()),
                mail.getSubject(),
                mail.getSenderEmail(),
                link.getRecipientEmail(),
                mail.getBodyCiphertext(),
                mail.getBodyE2eeAlgorithm(),
                link.getPasswordHint(),
                link.getExpiresAt(),
                mailAttachmentService.listForPublicSecureLink(mail.getId())
        );
    }

    public MailAttachmentService.PublicAttachmentDownload downloadPublicAttachment(
            String token,
            Long attachmentId,
            String ipAddress
    ) {
        MailExternalSecureLink link = requireActiveLink(token);
        MailMessage mail = requireEncryptedMail(link.getMailId());
        MailAttachmentService.PublicAttachmentDownload download = mailAttachmentService.downloadPublicSecureAttachment(
                mail.getId(),
                attachmentId
        );
        auditService.record(
                null,
                "MAIL_SECURE_LINK_ATTACHMENT_DOWNLOAD",
                "mailId=" + mail.getId() + ",attachment=" + attachmentId,
                ipAddress
        );
        return download;
    }

    public String buildNotificationSubject(MailMessage mail) {
        return "[MMMail Secure Mail] " + mail.getSubject();
    }

    public String buildNotificationBody(MailExternalSecureLink link, MailMessage mail) {
        StringBuilder builder = new StringBuilder();
        builder.append("You received a password-protected encrypted message from ")
                .append(mail.getSenderEmail())
                .append('.')
                .append('\n')
                .append('\n')
                .append("Open secure message: ")
                .append(link.getPublicUrl())
                .append('\n');
        if (StringUtils.hasText(link.getPasswordHint())) {
            builder.append("Password hint: ").append(link.getPasswordHint()).append('\n');
        }
        if (link.getExpiresAt() != null) {
            builder.append("Expires at: ")
                    .append(link.getExpiresAt().format(NOTIFICATION_TIME_FORMAT))
                    .append('\n');
        }
        builder.append('\n')
                .append("For security, the password is not included in this email. Ask the sender through another channel.");
        return builder.toString();
    }

    private MailExternalSecureLink findByMailId(Long mailId) {
        return mailExternalSecureLinkMapper.selectOne(new LambdaQueryWrapper<MailExternalSecureLink>()
                .eq(MailExternalSecureLink::getMailId, mailId)
                .last("limit 1"));
    }

    private MailExternalSecureLink requireActiveLink(String token) {
        String tokenHash = publicShareTokenCodec.hash(requireToken(token));
        MailExternalSecureLink link = mailExternalSecureLinkMapper.selectOne(new LambdaQueryWrapper<MailExternalSecureLink>()
                .eq(MailExternalSecureLink::getTokenHash, tokenHash)
                .last("limit 1"));
        if (link == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Mail secure link is not found");
        }
        validateLinkActive(link);
        return link;
    }

    private MailMessage requireEncryptedMail(Long mailId) {
        MailMessage mail = mailMessageMapper.selectById(mailId);
        if (mail == null || mail.getBodyE2eeEnabled() == null || mail.getBodyE2eeEnabled() != 1) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Mail secure link target is not found");
        }
        return mail;
    }

    private LocalDateTime validateExpiresAt(LocalDateTime expiresAt, LocalDateTime deliveryAt) {
        LocalDateTime baseline = deliveryAt == null ? LocalDateTime.now() : deliveryAt;
        if (expiresAt == null) {
            return baseline.plusDays(DEFAULT_EXPIRE_DAYS);
        }
        if (!expiresAt.isAfter(baseline)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Mail secure link expiresAt must be after delivery time");
        }
        if (expiresAt.isAfter(baseline.plusDays(MAX_EXPIRE_DAYS))) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Mail secure link expiresAt must be within 30 days");
        }
        return expiresAt;
    }

    private void validateLinkActive(MailExternalSecureLink link) {
        if (link.getRevokedAt() != null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Mail secure link has been revoked");
        }
        if (link.getExpiresAt() != null && !link.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Mail secure link has expired");
        }
    }

    private String buildPublicUrl(String publicBaseUrl, String token) {
        String baseUrl = requireText(publicBaseUrl, "Public base URL is required for secure mail delivery");
        String normalizedBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return normalizedBaseUrl + "/share/mail/" + token;
    }

    private String requireToken(String token) {
        return requireText(token, "Mail secure link token is required");
    }

    private String requireRecipientEmail(String recipientEmail) {
        return requireText(recipientEmail, "Recipient email is required").toLowerCase(Locale.ROOT);
    }

    private String normalizeMode(String mode) {
        return requireText(mode, "External Mail E2EE access mode is required").toUpperCase(Locale.ROOT);
    }

    private String normalizePasswordHint(String passwordHint) {
        return StringUtils.hasText(passwordHint) ? passwordHint.trim() : null;
    }

    private String requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, message);
        }
        return value.trim();
    }
}
