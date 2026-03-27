package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.mapper.PassAliasContactMapper;
import com.mmmail.server.mapper.PassMailAliasMapper;
import com.mmmail.server.mapper.UserAccountMapper;
import com.mmmail.server.model.dto.CreatePassAliasContactRequest;
import com.mmmail.server.model.dto.UpdatePassAliasContactRequest;
import com.mmmail.server.model.entity.PassAliasContact;
import com.mmmail.server.model.entity.PassMailAlias;
import com.mmmail.server.model.entity.UserAccount;
import com.mmmail.server.model.vo.PassAliasContactVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class PassAliasContactService {

    private static final int DEFAULT_FALSE = 0;
    private static final String REVERSE_ALIAS_DOMAIN = "reply.passmail.mmmail.local";
    private static final String RANDOM_ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int DEFAULT_LIMIT = 8;
    private static final int MAX_LIMIT = 20;

    private final PassAliasContactMapper passAliasContactMapper;
    private final PassMailAliasMapper passMailAliasMapper;
    private final UserAccountMapper userAccountMapper;
    private final AuditService auditService;

    public PassAliasContactService(
            PassAliasContactMapper passAliasContactMapper,
            PassMailAliasMapper passMailAliasMapper,
            UserAccountMapper userAccountMapper,
            AuditService auditService
    ) {
        this.passAliasContactMapper = passAliasContactMapper;
        this.passMailAliasMapper = passMailAliasMapper;
        this.userAccountMapper = userAccountMapper;
        this.auditService = auditService;
    }

    public List<PassAliasContactVo> listContacts(Long userId, Long aliasId, String ipAddress) {
        PassMailAlias alias = loadOwnedAlias(userId, aliasId);
        List<PassAliasContactVo> contacts = passAliasContactMapper.selectList(new LambdaQueryWrapper<PassAliasContact>()
                        .eq(PassAliasContact::getOwnerId, userId)
                        .eq(PassAliasContact::getAliasId, alias.getId())
                        .orderByDesc(PassAliasContact::getUpdatedAt))
                .stream()
                .map(this::toVo)
                .toList();
        auditService.record(userId, "PASS_ALIAS_CONTACT_LIST", "aliasId=" + aliasId + ",count=" + contacts.size(), ipAddress);
        return contacts;
    }

    @Transactional
    public PassAliasContactVo createContact(Long userId, Long aliasId, CreatePassAliasContactRequest request, String ipAddress) {
        PassMailAlias alias = loadOwnedAlias(userId, aliasId);
        UserAccount targetUser = requireTargetUser(request.targetEmail());
        ensureAliasContactAvailable(alias.getId(), normalizeEmail(targetUser.getEmail()), null);
        LocalDateTime now = LocalDateTime.now();

        PassAliasContact contact = new PassAliasContact();
        contact.setAliasId(alias.getId());
        contact.setOwnerId(userId);
        contact.setTargetUserId(targetUser.getId());
        contact.setTargetEmail(targetUser.getEmail());
        contact.setDisplayName(resolveDisplayName(request.displayName(), targetUser));
        contact.setNote(normalizeNote(request.note()));
        contact.setReverseAliasEmail(buildUniqueReverseAliasEmail());
        contact.setCreatedAt(now);
        contact.setUpdatedAt(now);
        contact.setDeleted(DEFAULT_FALSE);
        passAliasContactMapper.insert(contact);
        auditService.record(userId, "PASS_ALIAS_CONTACT_CREATE", "aliasId=" + aliasId + ",contactId=" + contact.getId(), ipAddress);
        return toVo(contact);
    }

    @Transactional
    public PassAliasContactVo updateContact(Long userId, Long aliasId, Long contactId, UpdatePassAliasContactRequest request, String ipAddress) {
        PassMailAlias alias = loadOwnedAlias(userId, aliasId);
        PassAliasContact contact = loadOwnedContact(userId, alias.getId(), contactId);
        UserAccount targetUser = requireTargetUser(request.targetEmail());
        ensureAliasContactAvailable(alias.getId(), normalizeEmail(targetUser.getEmail()), contact.getId());
        contact.setTargetUserId(targetUser.getId());
        contact.setTargetEmail(targetUser.getEmail());
        contact.setDisplayName(resolveDisplayName(request.displayName(), targetUser));
        contact.setNote(normalizeNote(request.note()));
        contact.setUpdatedAt(LocalDateTime.now());
        passAliasContactMapper.updateById(contact);
        auditService.record(userId, "PASS_ALIAS_CONTACT_UPDATE", "aliasId=" + aliasId + ",contactId=" + contactId, ipAddress);
        return toVo(contact);
    }

    @Transactional
    public void deleteContact(Long userId, Long aliasId, Long contactId, String ipAddress) {
        PassMailAlias alias = loadOwnedAlias(userId, aliasId);
        PassAliasContact contact = loadOwnedContact(userId, alias.getId(), contactId);
        passAliasContactMapper.deleteById(contact.getId());
        auditService.record(userId, "PASS_ALIAS_CONTACT_DELETE", "aliasId=" + aliasId + ",contactId=" + contactId, ipAddress);
    }

    public List<PassAliasContactVo> suggestContacts(Long userId, String senderEmail, String keyword, Integer limit) {
        PassMailAlias alias = requireOwnedEnabledAliasByEmail(userId, senderEmail);
        String normalizedKeyword = normalizeKeyword(keyword);
        int safeLimit = Math.max(1, Math.min(limit == null ? DEFAULT_LIMIT : limit, MAX_LIMIT));
        return passAliasContactMapper.selectList(new LambdaQueryWrapper<PassAliasContact>()
                        .eq(PassAliasContact::getOwnerId, userId)
                        .eq(PassAliasContact::getAliasId, alias.getId())
                        .orderByDesc(PassAliasContact::getUpdatedAt))
                .stream()
                .filter(contact -> matchesKeyword(contact, normalizedKeyword))
                .limit(safeLimit)
                .map(this::toVo)
                .toList();
    }

    public ResolvedReverseAliasTarget requireReverseAliasTarget(Long userId, String senderAliasEmail, String reverseAliasEmail) {
        PassMailAlias alias = requireOwnedEnabledAliasByEmail(userId, senderAliasEmail);
        String normalizedReverseAlias = normalizeEmail(reverseAliasEmail);
        PassAliasContact contact = passAliasContactMapper.selectOne(new LambdaQueryWrapper<PassAliasContact>()
                .eq(PassAliasContact::getOwnerId, userId)
                .eq(PassAliasContact::getAliasId, alias.getId())
                .eq(PassAliasContact::getReverseAliasEmail, normalizedReverseAlias)
                .last("limit 1"));
        if (contact == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Alias sender requires a reverse alias contact");
        }
        return new ResolvedReverseAliasTarget(contact.getTargetUserId(), contact.getTargetEmail(), contact.getReverseAliasEmail());
    }

    public boolean isOwnedEnabledAlias(Long userId, String senderEmail) {
        if (!StringUtils.hasText(senderEmail)) {
            return false;
        }
        return passMailAliasMapper.selectCount(new LambdaQueryWrapper<PassMailAlias>()
                .eq(PassMailAlias::getOwnerId, userId)
                .eq(PassMailAlias::getAliasEmail, normalizeEmail(senderEmail))
                .eq(PassMailAlias::getStatus, PassAliasService.STATUS_ENABLED)) > 0;
    }

    private PassMailAlias loadOwnedAlias(Long userId, Long aliasId) {
        PassMailAlias alias = passMailAliasMapper.selectOne(new LambdaQueryWrapper<PassMailAlias>()
                .eq(PassMailAlias::getId, aliasId)
                .eq(PassMailAlias::getOwnerId, userId)
                .last("limit 1"));
        if (alias == null) {
            throw new BizException(ErrorCode.PASS_ALIAS_NOT_FOUND, "Pass alias not found");
        }
        return alias;
    }

    private PassMailAlias requireOwnedEnabledAliasByEmail(Long userId, String senderEmail) {
        PassMailAlias alias = passMailAliasMapper.selectOne(new LambdaQueryWrapper<PassMailAlias>()
                .eq(PassMailAlias::getOwnerId, userId)
                .eq(PassMailAlias::getAliasEmail, normalizeEmail(senderEmail))
                .eq(PassMailAlias::getStatus, PassAliasService.STATUS_ENABLED)
                .last("limit 1"));
        if (alias == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Sender identity is unavailable");
        }
        return alias;
    }

    private PassAliasContact loadOwnedContact(Long userId, Long aliasId, Long contactId) {
        PassAliasContact contact = passAliasContactMapper.selectOne(new LambdaQueryWrapper<PassAliasContact>()
                .eq(PassAliasContact::getId, contactId)
                .eq(PassAliasContact::getOwnerId, userId)
                .eq(PassAliasContact::getAliasId, aliasId)
                .last("limit 1"));
        if (contact == null) {
            throw new BizException(ErrorCode.PASS_ALIAS_CONTACT_NOT_FOUND, "Pass alias contact not found");
        }
        return contact;
    }

    private void ensureAliasContactAvailable(Long aliasId, String targetEmail, Long ignoredContactId) {
        PassAliasContact existing = passAliasContactMapper.selectOne(new LambdaQueryWrapper<PassAliasContact>()
                .eq(PassAliasContact::getAliasId, aliasId)
                .eq(PassAliasContact::getTargetEmail, targetEmail)
                .last("limit 1"));
        if (existing != null && (ignoredContactId == null || !existing.getId().equals(ignoredContactId))) {
            throw new BizException(ErrorCode.PASS_ALIAS_CONTACT_ALREADY_EXISTS, "Pass alias contact already exists");
        }
    }

    private UserAccount requireTargetUser(String targetEmail) {
        String normalizedEmail = normalizeEmail(targetEmail);
        UserAccount user = userAccountMapper.selectOne(new LambdaQueryWrapper<UserAccount>()
                .eq(UserAccount::getEmail, normalizedEmail)
                .last("limit 1"));
        if (user == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Target email must belong to an existing MMMail user");
        }
        return user;
    }

    private boolean matchesKeyword(PassAliasContact contact, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        return contains(contact.getTargetEmail(), keyword)
                || contains(contact.getDisplayName(), keyword)
                || contains(contact.getReverseAliasEmail(), keyword);
    }

    private boolean contains(String value, String keyword) {
        return StringUtils.hasText(value) && value.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private String normalizeKeyword(String keyword) {
        return StringUtils.hasText(keyword) ? keyword.trim().toLowerCase(Locale.ROOT) : "";
    }

    private String resolveDisplayName(String displayName, UserAccount targetUser) {
        return StringUtils.hasText(displayName) ? displayName.trim() : targetUser.getDisplayName();
    }

    private String normalizeNote(String note) {
        return StringUtils.hasText(note) ? note.trim() : null;
    }

    private String normalizeEmail(String emailAddress) {
        return StringUtils.hasText(emailAddress) ? emailAddress.trim().toLowerCase(Locale.ROOT) : null;
    }

    private String buildUniqueReverseAliasEmail() {
        for (int attempt = 0; attempt < 20; attempt++) {
            String email = ("reply-" + randomSegment(12) + "@" + REVERSE_ALIAS_DOMAIN).toLowerCase(Locale.ROOT);
            if (passAliasContactMapper.selectCount(new LambdaQueryWrapper<PassAliasContact>()
                    .eq(PassAliasContact::getReverseAliasEmail, email)) == 0) {
                return email;
            }
        }
        throw new BizException(ErrorCode.INTERNAL_ERROR, "Unable to allocate reverse alias email");
    }

    private String randomSegment(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int index = 0; index < length; index++) {
            builder.append(RANDOM_ALPHABET.charAt(RANDOM.nextInt(RANDOM_ALPHABET.length())));
        }
        return builder.toString();
    }

    private PassAliasContactVo toVo(PassAliasContact contact) {
        return new PassAliasContactVo(
                String.valueOf(contact.getId()),
                String.valueOf(contact.getAliasId()),
                String.valueOf(contact.getTargetUserId()),
                contact.getTargetEmail(),
                contact.getDisplayName(),
                contact.getNote(),
                contact.getReverseAliasEmail(),
                contact.getCreatedAt(),
                contact.getUpdatedAt()
        );
    }

    public record ResolvedReverseAliasTarget(Long targetUserId, String targetEmail, String reverseAliasEmail) {
    }
}
