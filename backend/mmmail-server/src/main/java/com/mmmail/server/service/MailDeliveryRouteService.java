package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.mapper.UserAccountMapper;
import com.mmmail.server.model.entity.PassMailAlias;
import com.mmmail.server.model.entity.UserAccount;
import com.mmmail.server.model.vo.MailDeliveryTarget;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

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
        DeliveryResolutionContext context = new DeliveryResolutionContext(senderUserId, senderEmail, ipAddress, true);
        return resolveTargets(toEmail, context)
                .orElseThrow(() -> rejectUndeliverable(senderUserId, toEmail, ipAddress));
    }

    public List<MailDeliveryTarget> previewDeliveryTargets(Long senderUserId, String senderEmail, String toEmail) {
        DeliveryResolutionContext context = new DeliveryResolutionContext(senderUserId, senderEmail, null, false);
        return resolveTargets(toEmail, context).orElse(List.of());
    }

    private Optional<List<MailDeliveryTarget>> resolveTargets(String toEmail, DeliveryResolutionContext context) {
        String normalizedTarget = normalizeEmail(toEmail);
        if (!StringUtils.hasText(normalizedTarget)) {
            return Optional.empty();
        }
        if (passAliasContactService.isOwnedEnabledAlias(context.senderUserId(), context.senderUserEmail())) {
            var reverseAliasTarget = passAliasContactService.requireReverseAliasTarget(
                    context.senderUserId(),
                    context.senderUserEmail(),
                    normalizedTarget
            );
            recordReverseAliasRoute(context, reverseAliasTarget.reverseAliasEmail(), reverseAliasTarget.targetEmail());
            return resolveDirectOrAliasDeliveryTargets(
                    reverseAliasTarget.targetEmail(),
                    reverseAliasTarget.reverseAliasEmail(),
                    context
            );
        }
        return resolveDirectOrAliasDeliveryTargets(normalizedTarget, normalizedTarget, context);
    }

    private Optional<List<MailDeliveryTarget>> resolveDirectOrAliasDeliveryTargets(
            String resolvedEmail,
            String displayEmail,
            DeliveryResolutionContext context
    ) {
        UserAccount recipient = userAccountMapper.selectOne(new LambdaQueryWrapper<UserAccount>()
                .eq(UserAccount::getEmail, resolvedEmail)
                .last("limit 1"));
        if (recipient != null) {
            return Optional.of(List.of(MailDeliveryTarget.internal(recipient.getId(), displayEmail, recipient.getEmail())));
        }
        PassMailAlias alias = passAliasService.loadEnabledAliasByEmail(resolvedEmail);
        if (alias != null) {
            List<String> routeEmails = passAliasService.resolveRouteEmails(alias);
            if (routeEmails.isEmpty()) {
                throw new BizException(ErrorCode.INVALID_ARGUMENT, "Alias route is empty");
            }
            List<MailDeliveryTarget> targets = routeEmails.stream()
                    .map(routeEmail -> passMailboxService.resolveVerifiedRouteMailbox(alias.getOwnerId(), routeEmail))
                    .map(mailbox -> MailDeliveryTarget.internal(mailbox.getMailboxUserId(), alias.getAliasEmail(), mailbox.getMailboxEmail()))
                    .toList();
            String detail = "alias=" + alias.getAliasEmail() + ",routeCount=" + targets.size() + ",routes=" + String.join(",", routeEmails);
            recordAliasRelay(context, alias.getOwnerId(), detail);
            return Optional.of(targets);
        }
        return Optional.of(List.of(MailDeliveryTarget.smtp(displayEmail)));
    }

    private void recordReverseAliasRoute(DeliveryResolutionContext context, String reverseAliasEmail, String targetEmail) {
        if (!context.auditEnabled()) {
            return;
        }
        auditService.record(
                context.senderUserId(),
                "MAIL_ALIAS_REVERSE_ROUTE",
                "alias=" + context.senderUserEmail() + ",reverseAlias=" + reverseAliasEmail + ",target=" + targetEmail,
                context.ipAddress()
        );
    }

    private void recordAliasRelay(DeliveryResolutionContext context, Long aliasOwnerId, String detail) {
        if (!context.auditEnabled()) {
            return;
        }
        auditService.record(context.senderUserId(), "MAIL_ALIAS_RELAY", detail, context.ipAddress());
        if (!aliasOwnerId.equals(context.senderUserId())) {
            auditService.record(aliasOwnerId, "MAIL_ALIAS_RELAY", detail, context.ipAddress());
        }
    }

    private BizException rejectUndeliverable(Long senderUserId, String displayEmail, String ipAddress) {
        auditService.record(senderUserId, "MAIL_SEND_REJECTED", "recipient not found: " + displayEmail, ipAddress);
        return new BizException(ErrorCode.INVALID_ARGUMENT, "Unable to deliver mail");
    }

    private String normalizeEmail(String emailAddress) {
        return StringUtils.hasText(emailAddress) ? emailAddress.trim().toLowerCase(Locale.ROOT) : null;
    }

    private record DeliveryResolutionContext(
            Long senderUserId,
            String senderUserEmail,
            String ipAddress,
            boolean auditEnabled
    ) {
    }
}
