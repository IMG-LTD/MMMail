package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.mapper.MailMessageMapper;
import com.mmmail.server.mapper.PassAliasMailboxRouteMapper;
import com.mmmail.server.mapper.PassMailAliasMapper;
import com.mmmail.server.mapper.PassMailboxMapper;
import com.mmmail.server.mapper.UserAccountMapper;
import com.mmmail.server.model.dto.CreatePassMailboxRequest;
import com.mmmail.server.model.dto.VerifyPassMailboxRequest;
import com.mmmail.server.model.entity.MailMessage;
import com.mmmail.server.model.entity.PassAliasMailboxRoute;
import com.mmmail.server.model.entity.PassMailAlias;
import com.mmmail.server.model.entity.PassMailbox;
import com.mmmail.server.model.entity.UserAccount;
import com.mmmail.server.model.vo.PassMailboxVo;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

@Service
public class PassMailboxService {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_VERIFIED = "VERIFIED";

    private static final int DEFAULT_FALSE = 0;
    private static final int DEFAULT_TRUE = 1;
    private static final int CODE_LENGTH = 6;
    private static final String CODE_ALPHABET = "0123456789";
    private static final String VERIFICATION_SENDER = "no-reply@mmmail.local";
    private static final String VERIFICATION_SUBJECT = "MMMail mailbox verification code";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final PassMailboxMapper passMailboxMapper;
    private final PassAliasMailboxRouteMapper passAliasMailboxRouteMapper;
    private final PassMailAliasMapper passMailAliasMapper;
    private final UserAccountMapper userAccountMapper;
    private final MailMessageMapper mailMessageMapper;
    private final AuditService auditService;

    public PassMailboxService(
            PassMailboxMapper passMailboxMapper,
            PassAliasMailboxRouteMapper passAliasMailboxRouteMapper,
            PassMailAliasMapper passMailAliasMapper,
            UserAccountMapper userAccountMapper,
            MailMessageMapper mailMessageMapper,
            AuditService auditService
    ) {
        this.passMailboxMapper = passMailboxMapper;
        this.passAliasMailboxRouteMapper = passAliasMailboxRouteMapper;
        this.passMailAliasMapper = passMailAliasMapper;
        this.userAccountMapper = userAccountMapper;
        this.mailMessageMapper = mailMessageMapper;
        this.auditService = auditService;
    }

    public List<PassMailboxVo> listMailboxes(Long userId, String ipAddress) {
        UserAccount owner = requireUser(userId);
        ensurePrimaryMailbox(owner);
        List<PassMailboxVo> mailboxes = passMailboxMapper.selectList(new LambdaQueryWrapper<PassMailbox>()
                        .eq(PassMailbox::getOwnerId, userId)
                        .orderByDesc(PassMailbox::getIsDefault)
                        .orderByDesc(PassMailbox::getUpdatedAt))
                .stream()
                .map(mailbox -> toVo(mailbox, owner.getEmail()))
                .toList();
        auditService.record(userId, "PASS_MAILBOX_LIST", "count=" + mailboxes.size(), ipAddress);
        return mailboxes;
    }

    @Transactional
    public PassMailboxVo createMailbox(Long userId, CreatePassMailboxRequest request, String ipAddress) {
        UserAccount owner = requireUser(userId);
        ensurePrimaryMailbox(owner);
        UserAccount mailboxUser = requireMailboxUser(request.mailboxEmail());
        ensureMailboxAvailable(userId, mailboxUser.getEmail());
        PassMailbox mailbox = buildPendingMailbox(userId, mailboxUser, LocalDateTime.now());
        try {
            passMailboxMapper.insert(mailbox);
        } catch (DuplicateKeyException ignored) {
        }
        sendVerificationMail(owner, mailboxUser, mailbox);
        auditService.record(userId, "PASS_MAILBOX_CREATE", "mailbox=" + mailbox.getMailboxEmail(), ipAddress);
        return toVo(mailbox, owner.getEmail());
    }

    @Transactional
    public PassMailboxVo verifyMailbox(Long userId, Long mailboxId, VerifyPassMailboxRequest request, String ipAddress) {
        UserAccount owner = requireUser(userId);
        ensurePrimaryMailbox(owner);
        PassMailbox mailbox = loadOwnedMailbox(userId, mailboxId);
        if (STATUS_VERIFIED.equals(mailbox.getStatus())) {
            return toVo(mailbox, owner.getEmail());
        }
        validateVerificationCode(mailbox, request.verificationCode());
        markVerified(mailbox, shouldBecomeDefault(userId));
        passMailboxMapper.updateById(mailbox);
        auditService.record(userId, "PASS_MAILBOX_VERIFY", "mailboxId=" + mailboxId, ipAddress);
        return toVo(mailbox, owner.getEmail());
    }

    @Transactional
    public PassMailboxVo setDefaultMailbox(Long userId, Long mailboxId, String ipAddress) {
        UserAccount owner = requireUser(userId);
        ensurePrimaryMailbox(owner);
        PassMailbox mailbox = loadOwnedMailbox(userId, mailboxId);
        requireVerified(mailbox);
        clearDefaultMailbox(userId);
        mailbox.setIsDefault(DEFAULT_TRUE);
        mailbox.setUpdatedAt(LocalDateTime.now());
        passMailboxMapper.updateById(mailbox);
        auditService.record(userId, "PASS_MAILBOX_SET_DEFAULT", "mailboxId=" + mailboxId, ipAddress);
        return toVo(mailbox, owner.getEmail());
    }

    @Transactional
    public void deleteMailbox(Long userId, Long mailboxId, String ipAddress) {
        UserAccount owner = requireUser(userId);
        ensurePrimaryMailbox(owner);
        PassMailbox mailbox = loadOwnedMailbox(userId, mailboxId);
        rejectPrimaryMailboxDelete(owner, mailbox);
        ensureMailboxUnused(userId, mailbox.getMailboxEmail());
        boolean defaultMailbox = mailbox.getIsDefault() != null && mailbox.getIsDefault() == DEFAULT_TRUE;
        passMailboxMapper.deleteById(mailbox.getId());
        if (defaultMailbox) {
            assignFallbackDefaultMailbox(userId);
        }
        auditService.record(userId, "PASS_MAILBOX_DELETE", "mailboxId=" + mailboxId, ipAddress);
    }

    public String resolveAliasForwardTarget(Long userId, String requestedEmail) {
        return resolveAliasForwardTargets(userId, requestedEmail, null).get(0).getMailboxEmail();
    }

    public List<PassMailbox> resolveAliasForwardTargets(Long userId, String requestedEmail, List<String> requestedEmails) {
        UserAccount owner = requireUser(userId);
        ensurePrimaryMailbox(owner);
        List<String> normalized = normalizeRequestedRouteEmails(requestedEmail, requestedEmails);
        if (normalized.isEmpty()) {
            return List.of(loadDefaultMailbox(userId));
        }
        List<PassMailbox> result = new ArrayList<>();
        for (String email : normalized) {
            result.add(requireVerifiedMailbox(userId, email));
        }
        return result;
    }

    public PassMailbox resolveVerifiedRouteMailbox(Long userId, String mailboxEmail) {
        UserAccount owner = requireUser(userId);
        ensurePrimaryMailbox(owner);
        return requireVerifiedMailbox(userId, mailboxEmail);
    }

    public void ensurePrimaryMailbox(Long userId) {
        ensurePrimaryMailbox(requireUser(userId));
    }

    private void ensurePrimaryMailbox(UserAccount owner) {
        PassMailbox mailbox = findMailbox(owner.getId(), owner.getEmail());
        if (mailbox == null) {
            insertPrimaryMailbox(owner);
            return;
        }
        promotePrimaryMailbox(owner, mailbox);
    }

    private void insertPrimaryMailbox(UserAccount owner) {
        LocalDateTime now = LocalDateTime.now();
        PassMailbox mailbox = new PassMailbox();
        mailbox.setOwnerId(owner.getId());
        mailbox.setMailboxUserId(owner.getId());
        mailbox.setMailboxEmail(normalizeEmail(owner.getEmail()));
        mailbox.setStatus(STATUS_VERIFIED);
        mailbox.setVerificationCode(null);
        mailbox.setVerificationSentAt(now);
        mailbox.setVerifiedAt(now);
        mailbox.setIsDefault(hasDefaultMailbox(owner.getId()) ? DEFAULT_FALSE : DEFAULT_TRUE);
        mailbox.setCreatedAt(now);
        mailbox.setUpdatedAt(now);
        mailbox.setDeleted(DEFAULT_FALSE);
        try {
            passMailboxMapper.insert(mailbox);
        } catch (DuplicateKeyException ignored) {
        }
    }

    private void promotePrimaryMailbox(UserAccount owner, PassMailbox mailbox) {
        boolean changed = false;
        if (!STATUS_VERIFIED.equals(mailbox.getStatus())) {
            mailbox.setStatus(STATUS_VERIFIED);
            mailbox.setVerifiedAt(LocalDateTime.now());
            changed = true;
        }
        if (!owner.getId().equals(mailbox.getMailboxUserId())) {
            mailbox.setMailboxUserId(owner.getId());
            changed = true;
        }
        if (mailbox.getVerificationCode() != null) {
            mailbox.setVerificationCode(null);
            changed = true;
        }
        if (!hasDefaultMailbox(owner.getId()) && (mailbox.getIsDefault() == null || mailbox.getIsDefault() == DEFAULT_FALSE)) {
            mailbox.setIsDefault(DEFAULT_TRUE);
            changed = true;
        }
        if (changed) {
            mailbox.setUpdatedAt(LocalDateTime.now());
            passMailboxMapper.updateById(mailbox);
        }
    }

    private PassMailbox buildPendingMailbox(Long userId, UserAccount mailboxUser, LocalDateTime now) {
        PassMailbox mailbox = new PassMailbox();
        mailbox.setOwnerId(userId);
        mailbox.setMailboxUserId(mailboxUser.getId());
        mailbox.setMailboxEmail(mailboxUser.getEmail());
        mailbox.setStatus(STATUS_PENDING);
        mailbox.setVerificationCode(randomCode(CODE_LENGTH));
        mailbox.setVerificationSentAt(now);
        mailbox.setVerifiedAt(null);
        mailbox.setIsDefault(DEFAULT_FALSE);
        mailbox.setCreatedAt(now);
        mailbox.setUpdatedAt(now);
        mailbox.setDeleted(DEFAULT_FALSE);
        return mailbox;
    }

    private void sendVerificationMail(UserAccount owner, UserAccount mailboxUser, PassMailbox mailbox) {
        LocalDateTime now = LocalDateTime.now();
        MailMessage inbox = new MailMessage();
        inbox.setOwnerId(mailboxUser.getId());
        inbox.setPeerId(owner.getId());
        inbox.setPeerEmail(VERIFICATION_SENDER);
        inbox.setSenderEmail(VERIFICATION_SENDER);
        inbox.setDirection("IN");
        inbox.setFolderType("INBOX");
        inbox.setSubject(VERIFICATION_SUBJECT);
        inbox.setBodyCiphertext(buildVerificationBody(owner.getEmail(), mailbox.getVerificationCode()));
        inbox.setIsRead(DEFAULT_FALSE);
        inbox.setIsStarred(DEFAULT_FALSE);
        inbox.setIsDraft(DEFAULT_FALSE);
        inbox.setLabelsJson("[]");
        inbox.setIdempotencyKey("pass-mailbox-verification-" + mailbox.getId() + '-' + mailbox.getVerificationSentAt());
        inbox.setSentAt(now);
        inbox.setCreatedAt(now);
        inbox.setUpdatedAt(now);
        inbox.setDeleted(DEFAULT_FALSE);
        mailMessageMapper.insert(inbox);
    }

    private String buildVerificationBody(String ownerEmail, String verificationCode) {
        return "Mailbox verification for " + ownerEmail + "\n\nVerification code: " + verificationCode;
    }

    private void validateVerificationCode(PassMailbox mailbox, String verificationCode) {
        String normalized = StringUtils.hasText(verificationCode) ? verificationCode.trim() : "";
        if (!normalized.equals(mailbox.getVerificationCode())) {
            throw new BizException(ErrorCode.PASS_MAILBOX_VERIFICATION_INVALID, "Verification code is invalid");
        }
    }

    private void markVerified(PassMailbox mailbox, boolean defaultMailbox) {
        mailbox.setStatus(STATUS_VERIFIED);
        mailbox.setVerificationCode(null);
        mailbox.setVerifiedAt(LocalDateTime.now());
        mailbox.setIsDefault(defaultMailbox ? DEFAULT_TRUE : DEFAULT_FALSE);
        mailbox.setUpdatedAt(LocalDateTime.now());
    }

    private boolean shouldBecomeDefault(Long userId) {
        return !hasDefaultMailbox(userId);
    }

    private void clearDefaultMailbox(Long userId) {
        passMailboxMapper.update(
                null,
                new LambdaUpdateWrapper<PassMailbox>()
                        .eq(PassMailbox::getOwnerId, userId)
                        .set(PassMailbox::getIsDefault, DEFAULT_FALSE)
                        .set(PassMailbox::getUpdatedAt, LocalDateTime.now())
        );
    }

    private void assignFallbackDefaultMailbox(Long userId) {
        PassMailbox mailbox = passMailboxMapper.selectOne(new LambdaQueryWrapper<PassMailbox>()
                .eq(PassMailbox::getOwnerId, userId)
                .eq(PassMailbox::getStatus, STATUS_VERIFIED)
                .orderByAsc(PassMailbox::getMailboxEmail)
                .last("limit 1"));
        if (mailbox == null) {
            throw new BizException(ErrorCode.PASS_MAILBOX_NOT_FOUND, "No verified mailbox available");
        }
        mailbox.setIsDefault(DEFAULT_TRUE);
        mailbox.setUpdatedAt(LocalDateTime.now());
        passMailboxMapper.updateById(mailbox);
    }

    private PassMailbox loadOwnedMailbox(Long userId, Long mailboxId) {
        PassMailbox mailbox = passMailboxMapper.selectOne(new LambdaQueryWrapper<PassMailbox>()
                .eq(PassMailbox::getId, mailboxId)
                .eq(PassMailbox::getOwnerId, userId)
                .last("limit 1"));
        if (mailbox == null) {
            throw new BizException(ErrorCode.PASS_MAILBOX_NOT_FOUND, "Pass mailbox not found");
        }
        return mailbox;
    }

    private PassMailbox loadDefaultMailbox(Long userId) {
        PassMailbox mailbox = passMailboxMapper.selectOne(new LambdaQueryWrapper<PassMailbox>()
                .eq(PassMailbox::getOwnerId, userId)
                .eq(PassMailbox::getStatus, STATUS_VERIFIED)
                .eq(PassMailbox::getIsDefault, DEFAULT_TRUE)
                .last("limit 1"));
        if (mailbox != null) {
            return mailbox;
        }
        throw new BizException(ErrorCode.PASS_MAILBOX_NOT_FOUND, "No default mailbox available");
    }

    private PassMailbox requireVerifiedMailbox(Long userId, String mailboxEmail) {
        String normalized = normalizeEmail(mailboxEmail);
        PassMailbox mailbox = findMailbox(userId, normalized);
        if (mailbox == null) {
            throw new BizException(ErrorCode.PASS_MAILBOX_NOT_FOUND, "Pass mailbox not found");
        }
        requireVerified(mailbox);
        return mailbox;
    }

    private void requireVerified(PassMailbox mailbox) {
        if (!STATUS_VERIFIED.equals(mailbox.getStatus())) {
            throw new BizException(ErrorCode.PASS_MAILBOX_NOT_VERIFIED, "Mailbox must be verified first");
        }
    }

    private void rejectPrimaryMailboxDelete(UserAccount owner, PassMailbox mailbox) {
        if (normalizeEmail(owner.getEmail()).equals(mailbox.getMailboxEmail())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Primary mailbox cannot be deleted");
        }
    }

    private void ensureMailboxUnused(Long userId, String mailboxEmail) {
        long routeCount = passAliasMailboxRouteMapper.selectCount(new LambdaQueryWrapper<PassAliasMailboxRoute>()
                .eq(PassAliasMailboxRoute::getOwnerId, userId)
                .eq(PassAliasMailboxRoute::getMailboxEmail, mailboxEmail));
        long legacyCount = passMailAliasMapper.selectCount(new LambdaQueryWrapper<PassMailAlias>()
                .eq(PassMailAlias::getOwnerId, userId)
                .eq(PassMailAlias::getForwardToEmail, mailboxEmail));
        if (routeCount > 0 || legacyCount > 0) {
            throw new BizException(ErrorCode.PASS_MAILBOX_IN_USE, "Mailbox is still used by aliases");
        }
    }

    private void ensureMailboxAvailable(Long userId, String mailboxEmail) {
        if (findMailbox(userId, mailboxEmail) != null) {
            throw new BizException(ErrorCode.PASS_MAILBOX_ALREADY_EXISTS, "Pass mailbox already exists");
        }
    }

    private List<String> normalizeRequestedRouteEmails(String requestedEmail, List<String> requestedEmails) {
        LinkedHashSet<String> emails = new LinkedHashSet<>();
        if (requestedEmails != null) {
            for (String email : requestedEmails) {
                String normalized = normalizeEmail(email);
                if (StringUtils.hasText(normalized)) {
                    emails.add(normalized);
                }
            }
            if (!requestedEmails.isEmpty() && emails.isEmpty()) {
                throw new BizException(ErrorCode.INVALID_ARGUMENT, "At least one mailbox route is required");
            }
        }
        String normalizedSingle = normalizeEmail(requestedEmail);
        if (StringUtils.hasText(normalizedSingle) && emails.isEmpty()) {
            emails.add(normalizedSingle);
        }
        return List.copyOf(emails);
    }

    private PassMailbox findMailbox(Long userId, String mailboxEmail) {
        return passMailboxMapper.selectOne(new LambdaQueryWrapper<PassMailbox>()
                .eq(PassMailbox::getOwnerId, userId)
                .eq(PassMailbox::getMailboxEmail, normalizeEmail(mailboxEmail))
                .last("limit 1"));
    }

    private boolean hasDefaultMailbox(Long userId) {
        return passMailboxMapper.selectCount(new LambdaQueryWrapper<PassMailbox>()
                .eq(PassMailbox::getOwnerId, userId)
                .eq(PassMailbox::getStatus, STATUS_VERIFIED)
                .eq(PassMailbox::getIsDefault, DEFAULT_TRUE)) > 0;
    }

    private UserAccount requireUser(Long userId) {
        UserAccount user = userAccountMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }

    private UserAccount requireMailboxUser(String mailboxEmail) {
        UserAccount user = userAccountMapper.selectOne(new LambdaQueryWrapper<UserAccount>()
                .eq(UserAccount::getEmail, normalizeEmail(mailboxEmail))
                .last("limit 1"));
        if (user == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Mailbox email must belong to an existing MMMail user");
        }
        return user;
    }

    private String normalizeEmail(String emailAddress) {
        return StringUtils.hasText(emailAddress) ? emailAddress.trim().toLowerCase(Locale.ROOT) : null;
    }

    private String randomCode(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int index = 0; index < length; index++) {
            builder.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
        }
        return builder.toString();
    }

    private PassMailboxVo toVo(PassMailbox mailbox, String ownerEmail) {
        return new PassMailboxVo(
                String.valueOf(mailbox.getId()),
                mailbox.getMailboxEmail(),
                mailbox.getStatus(),
                mailbox.getIsDefault() != null && mailbox.getIsDefault() == DEFAULT_TRUE,
                normalizeEmail(ownerEmail).equals(mailbox.getMailboxEmail()),
                mailbox.getCreatedAt(),
                mailbox.getUpdatedAt(),
                mailbox.getVerifiedAt()
        );
    }
}
