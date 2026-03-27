package com.mmmail.server.service;

import com.mmmail.server.mapper.MailMessageMapper;
import com.mmmail.server.model.entity.MailMessage;
import com.mmmail.server.model.entity.OrgMember;
import com.mmmail.server.model.entity.OrgWorkspace;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class OrgSecurityReminderMailService {

    private static final String REMINDER_SENDER = "security@mmmail.local";

    private final MailMessageMapper mailMessageMapper;

    public OrgSecurityReminderMailService(MailMessageMapper mailMessageMapper) {
        this.mailMessageMapper = mailMessageMapper;
    }

    public void sendTwoFactorReminder(
            OrgWorkspace org,
            OrgMember actor,
            OrgMember target,
            String enforcementLevel,
            int gracePeriodDays
    ) {
        LocalDateTime now = LocalDateTime.now();
        MailMessage message = new MailMessage();
        message.setOwnerId(target.getUserId());
        message.setPeerId(actor.getUserId());
        message.setPeerEmail(actor.getUserEmail());
        message.setSenderEmail(REMINDER_SENDER);
        message.setDirection("IN");
        message.setFolderType("INBOX");
        message.setSubject(buildSubject(org.getName()));
        message.setBodyCiphertext(buildBody(org.getName(), actor.getUserEmail(), enforcementLevel, gracePeriodDays));
        message.setIsRead(0);
        message.setIsStarred(0);
        message.setIsDraft(0);
        message.setLabelsJson("[]");
        message.setIdempotencyKey(buildIdempotencyKey(org.getId(), target.getId(), now));
        message.setSentAt(now);
        message.setCreatedAt(now);
        message.setUpdatedAt(now);
        message.setDeleted(0);
        mailMessageMapper.insert(message);
    }

    private String buildSubject(String orgName) {
        return "[MMMail Security] Enable two-factor authentication for " + orgName;
    }

    private String buildBody(String orgName, String actorEmail, String enforcementLevel, int gracePeriodDays) {
        String gracePeriodLine = gracePeriodDays > 0
                ? "Grace period: %d day(s) before organization-scoped access is restricted.%n".formatted(gracePeriodDays)
                : "Grace period: none. Organization-scoped access can be restricted immediately.%n";
        return """
                Your organization "%s" requires stronger sign-in protection.

                A manager (%s) asked you to enable two-factor authentication before continuing to access organization-scoped products.

                Current enforcement scope: %s
                %s
                Next step: open /authenticator and add at least one TOTP entry.
                """.formatted(orgName, actorEmail, enforcementLevel, gracePeriodLine);
    }

    private String buildIdempotencyKey(Long orgId, Long memberId, LocalDateTime now) {
        return "org-auth-2fa-reminder-%d-%d-%d".formatted(orgId, memberId, now.toEpochSecond(java.time.ZoneOffset.UTC));
    }
}
