package com.mmmail.server.service;

import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.mapper.UserAccountMapper;
import com.mmmail.server.model.entity.UserAccount;
import com.mmmail.server.model.vo.MailSenderIdentityVo;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class MailSenderIdentityService {

    private static final String SOURCE_PRIMARY = "PRIMARY";
    private static final String SOURCE_ORG_CUSTOM_DOMAIN = "ORG_CUSTOM_DOMAIN";
    private static final String SOURCE_PASS_ALIAS = "PASS_ALIAS";
    private static final String STATUS_ENABLED = "ENABLED";

    private final UserAccountMapper userAccountMapper;
    private final OrgMailIdentityService orgMailIdentityService;
    private final PassAliasService passAliasService;
    private final AuditService auditService;

    public MailSenderIdentityService(
            UserAccountMapper userAccountMapper,
            OrgMailIdentityService orgMailIdentityService,
            PassAliasService passAliasService,
            AuditService auditService
    ) {
        this.userAccountMapper = userAccountMapper;
        this.orgMailIdentityService = orgMailIdentityService;
        this.passAliasService = passAliasService;
        this.auditService = auditService;
    }

    public List<MailSenderIdentityVo> listSenderIdentities(Long userId, String ipAddress) {
        UserAccount user = requireUser(userId);
        List<MailSenderIdentityVo> orgIdentities = orgMailIdentityService.listEnabledOrgSenderIdentities(userId);
        List<MailSenderIdentityVo> aliasIdentities = passAliasService.listAliasSenderIdentities(userId);
        boolean hasDefaultIdentity = orgIdentities.stream().anyMatch(MailSenderIdentityVo::defaultIdentity);

        List<MailSenderIdentityVo> options = new ArrayList<>();
        options.add(new MailSenderIdentityVo(
                null,
                null,
                null,
                null,
                user.getEmail(),
                user.getDisplayName(),
                SOURCE_PRIMARY,
                STATUS_ENABLED,
                !hasDefaultIdentity
        ));
        options.addAll(orgIdentities);
        options.addAll(aliasIdentities);

        List<MailSenderIdentityVo> sorted = options.stream()
                .sorted(Comparator.comparing(MailSenderIdentityVo::defaultIdentity).reversed()
                        .thenComparing(identity -> sourceRank(identity.source()))
                        .thenComparing(MailSenderIdentityVo::emailAddress))
                .toList();
        auditService.record(userId, "MAIL_IDENTITY_LIST", "count=" + sorted.size(), ipAddress);
        return sorted;
    }

    public String resolveAuthorizedSenderEmail(Long userId, String fromEmail) {
        UserAccount user = requireUser(userId);
        if (!StringUtils.hasText(fromEmail)) {
            return user.getEmail();
        }
        String normalized = fromEmail.trim().toLowerCase(Locale.ROOT);
        if (normalized.equals(user.getEmail().trim().toLowerCase(Locale.ROOT))) {
            return user.getEmail();
        }
        String orgIdentity = orgMailIdentityService.resolveAuthorizedOrgSenderEmailOrNull(userId, normalized);
        if (orgIdentity != null) {
            return orgIdentity;
        }
        String aliasIdentity = passAliasService.resolveAuthorizedAliasEmailOrNull(userId, normalized);
        if (aliasIdentity != null) {
            return aliasIdentity;
        }
        throw new BizException(ErrorCode.INVALID_ARGUMENT, "Sender identity is unavailable");
    }

    private UserAccount requireUser(Long userId) {
        UserAccount user = userAccountMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }

    private int sourceRank(String source) {
        if (SOURCE_PRIMARY.equals(source)) {
            return 0;
        }
        if (SOURCE_ORG_CUSTOM_DOMAIN.equals(source)) {
            return 1;
        }
        if (SOURCE_PASS_ALIAS.equals(source)) {
            return 2;
        }
        return 3;
    }
}
