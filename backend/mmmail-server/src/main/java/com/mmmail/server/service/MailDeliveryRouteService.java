package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.mapper.UserAccountMapper;
import com.mmmail.server.model.entity.PassMailAlias;
import com.mmmail.server.model.entity.PassMailbox;
import com.mmmail.server.model.entity.UserAccount;
import com.mmmail.server.model.vo.MailDeliveryTarget;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

@Service
public class MailDeliveryRouteService {

    private final UserAccountMapper userAccountMapper;
    private final PassAliasService passAliasService;
    private final PassAliasContactService passAliasContactService;
    private final PassMailboxService passMailboxService;
    private final AuditService auditService;

    public MailDeliveryRouteService(
            UserAccountMapper userAccountMapper,
            PassAliasService passAliasService,
            PassAliasContactService passAliasContactService,
            PassMailboxService passMailboxService,
            AuditService auditService
    ) {
        this.userAccountMapper = userAccountMapper;
        this.passAliasService = passAliasService;
        this.passAliasContactService = passAliasContactService;
        this.passMailboxService = passMailboxService;
        this.auditService = auditService;
    }

    public List<MailDeliveryTarget> resolveDeliveryTargets(Long senderUserId, String senderEmail, String toEmail, String ipAddress) {
        String normalizedTarget = normalizeEmail(toEmail);
        if (passAliasContactService.isOwnedEnabledAlias(senderUserId, senderEmail)) {
            var reverseAliasTarget = passAliasContactService.requireReverseAliasTarget(senderUserId, senderEmail, normalizedTarget);
            auditService.record(
                    senderUserId,
                    "MAIL_ALIAS_REVERSE_ROUTE",
                    "alias=" + senderEmail + ",reverseAlias=" + reverseAliasTarget.reverseAliasEmail() + ",target=" + reverseAliasTarget.targetEmail(),
                    ipAddress
            );
            return resolveDirectOrAliasDeliveryTargets(reverseAliasTarget.targetEmail(), reverseAliasTarget.reverseAliasEmail(), ipAddress, senderUserId);
        }
        return resolveDirectOrAliasDeliveryTargets(normalizedTarget, normalizedTarget, ipAddress, senderUserId);
    }

    private List<MailDeliveryTarget> resolveDirectOrAliasDeliveryTargets(
            String resolvedEmail,
            String displayEmail,
            String ipAddress,
            Long senderUserId
    ) {
        UserAccount recipient = userAccountMapper.selectOne(new LambdaQueryWrapper<UserAccount>()
                .eq(UserAccount::getEmail, resolvedEmail)
                .last("limit 1"));
        if (recipient != null) {
            return List.of(new MailDeliveryTarget(recipient.getId(), displayEmail, recipient.getEmail()));
        }
        PassMailAlias alias = passAliasService.loadEnabledAliasByEmail(resolvedEmail);
        if (alias != null) {
            List<String> routeEmails = passAliasService.resolveRouteEmails(alias);
            if (routeEmails.isEmpty()) {
                throw new BizException(ErrorCode.INVALID_ARGUMENT, "Alias route is empty");
            }
            List<MailDeliveryTarget> targets = routeEmails.stream()
                    .map(routeEmail -> passMailboxService.resolveVerifiedRouteMailbox(alias.getOwnerId(), routeEmail))
                    .map(mailbox -> new MailDeliveryTarget(mailbox.getMailboxUserId(), alias.getAliasEmail(), mailbox.getMailboxEmail()))
                    .toList();
            String detail = "alias=" + alias.getAliasEmail() + ",routeCount=" + targets.size() + ",routes=" + String.join(",", routeEmails);
            auditService.record(senderUserId, "MAIL_ALIAS_RELAY", detail, ipAddress);
            if (!alias.getOwnerId().equals(senderUserId)) {
                auditService.record(alias.getOwnerId(), "MAIL_ALIAS_RELAY", detail, ipAddress);
            }
            return targets;
        }
        auditService.record(senderUserId, "MAIL_SEND_REJECTED", "recipient not found: " + displayEmail, ipAddress);
        throw new BizException(ErrorCode.INVALID_ARGUMENT, "Unable to deliver mail");
    }

    private String normalizeEmail(String emailAddress) {
        return StringUtils.hasText(emailAddress) ? emailAddress.trim().toLowerCase(Locale.ROOT) : null;
    }
}
