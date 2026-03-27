package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mmmail.server.mapper.OrgCustomDomainMapper;
import com.mmmail.server.mapper.PassAliasContactMapper;
import com.mmmail.server.mapper.PassMailAliasMapper;
import com.mmmail.server.mapper.PassMailboxMapper;
import com.mmmail.server.mapper.SimpleLoginRelayPolicyMapper;
import com.mmmail.server.model.entity.OrgCustomDomain;
import com.mmmail.server.model.entity.PassAliasContact;
import com.mmmail.server.model.entity.PassMailAlias;
import com.mmmail.server.model.entity.PassMailbox;
import com.mmmail.server.model.entity.SimpleLoginRelayPolicy;
import com.mmmail.server.model.vo.SimpleLoginOverviewVo;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class SimpleLoginService {

    private static final String ALIAS_STATUS_ENABLED = "ENABLED";
    private static final String MAILBOX_STATUS_VERIFIED = "VERIFIED";
    private static final String DOMAIN_STATUS_VERIFIED = "VERIFIED";
    private static final int DEFAULT_TRUE = 1;

    private final PassMailAliasMapper passMailAliasMapper;
    private final PassMailboxMapper passMailboxMapper;
    private final PassAliasContactMapper passAliasContactMapper;
    private final OrgCustomDomainMapper orgCustomDomainMapper;
    private final SimpleLoginRelayPolicyMapper simpleLoginRelayPolicyMapper;
    private final OrgAccessService orgAccessService;
    private final PassMailboxService passMailboxService;
    private final AuditService auditService;

    public SimpleLoginService(
            PassMailAliasMapper passMailAliasMapper,
            PassMailboxMapper passMailboxMapper,
            PassAliasContactMapper passAliasContactMapper,
            OrgCustomDomainMapper orgCustomDomainMapper,
            SimpleLoginRelayPolicyMapper simpleLoginRelayPolicyMapper,
            OrgAccessService orgAccessService,
            PassMailboxService passMailboxService,
            AuditService auditService
    ) {
        this.passMailAliasMapper = passMailAliasMapper;
        this.passMailboxMapper = passMailboxMapper;
        this.passAliasContactMapper = passAliasContactMapper;
        this.orgCustomDomainMapper = orgCustomDomainMapper;
        this.simpleLoginRelayPolicyMapper = simpleLoginRelayPolicyMapper;
        this.orgAccessService = orgAccessService;
        this.passMailboxService = passMailboxService;
        this.auditService = auditService;
    }

    public SimpleLoginOverviewVo getOverview(Long userId, Long orgId, String ipAddress) {
        passMailboxService.ensurePrimaryMailbox(userId);
        List<PassMailAlias> aliases = listAliases(userId);
        List<PassMailbox> mailboxes = listMailboxes(userId);
        long reverseAliasContactCount = countReverseAliasContacts(userId);
        OrgScope orgScope = loadOrgScope(userId, orgId);
        auditService.record(userId, "SIMPLELOGIN_OVERVIEW", buildAuditDetail(orgId, aliases, mailboxes, reverseAliasContactCount, orgScope), ipAddress, orgId);
        return buildOverview(orgId, aliases, mailboxes, reverseAliasContactCount, orgScope);
    }

    private List<PassMailAlias> listAliases(Long userId) {
        return passMailAliasMapper.selectList(new LambdaQueryWrapper<PassMailAlias>()
                .eq(PassMailAlias::getOwnerId, userId)
                .orderByDesc(PassMailAlias::getUpdatedAt));
    }

    private List<PassMailbox> listMailboxes(Long userId) {
        return passMailboxMapper.selectList(new LambdaQueryWrapper<PassMailbox>()
                .eq(PassMailbox::getOwnerId, userId)
                .orderByDesc(PassMailbox::getIsDefault)
                .orderByDesc(PassMailbox::getUpdatedAt));
    }

    private long countReverseAliasContacts(Long userId) {
        return safeCount(passAliasContactMapper.selectCount(new LambdaQueryWrapper<PassAliasContact>()
                .eq(PassAliasContact::getOwnerId, userId)));
    }

    private OrgScope loadOrgScope(Long userId, Long orgId) {
        if (orgId == null) {
            return new OrgScope(List.of(), List.of());
        }
        orgAccessService.requireActiveMember(userId, orgId);
        List<OrgCustomDomain> domains = orgCustomDomainMapper.selectList(new LambdaQueryWrapper<OrgCustomDomain>()
                .eq(OrgCustomDomain::getOrgId, orgId)
                .orderByDesc(OrgCustomDomain::getIsDefault)
                .orderByDesc(OrgCustomDomain::getUpdatedAt));
        List<SimpleLoginRelayPolicy> policies = simpleLoginRelayPolicyMapper.selectList(new LambdaQueryWrapper<SimpleLoginRelayPolicy>()
                .eq(SimpleLoginRelayPolicy::getOrgId, orgId)
                .orderByDesc(SimpleLoginRelayPolicy::getUpdatedAt));
        return new OrgScope(domains, policies);
    }

    private String buildAuditDetail(
            Long orgId,
            List<PassMailAlias> aliases,
            List<PassMailbox> mailboxes,
            long reverseAliasContactCount,
            OrgScope orgScope
    ) {
        return "orgId=" + orgId
                + ",aliases=" + aliases.size()
                + ",mailboxes=" + mailboxes.size()
                + ",contacts=" + reverseAliasContactCount
                + ",policies=" + orgScope.policies().size();
    }

    private SimpleLoginOverviewVo buildOverview(
            Long orgId,
            List<PassMailAlias> aliases,
            List<PassMailbox> mailboxes,
            long reverseAliasContactCount,
            OrgScope orgScope
    ) {
        long aliasCount = aliases.size();
        long enabledAliasCount = aliases.stream().filter(alias -> ALIAS_STATUS_ENABLED.equals(alias.getStatus())).count();
        long mailboxCount = mailboxes.size();
        long verifiedMailboxCount = mailboxes.stream().filter(mailbox -> MAILBOX_STATUS_VERIFIED.equals(mailbox.getStatus())).count();
        long verifiedCustomDomainCount = orgScope.domains().stream().filter(domain -> DOMAIN_STATUS_VERIFIED.equals(domain.getStatus())).count();
        long relayPolicyCount = orgScope.policies().size();
        long catchAllDomainCount = orgScope.policies().stream().filter(policy -> Integer.valueOf(DEFAULT_TRUE).equals(policy.getCatchAllEnabled())).count();
        long subdomainPolicyCount = orgScope.policies().stream()
                .filter(policy -> !SimpleLoginRelayPolicyService.SUBDOMAIN_DISABLED.equals(policy.getSubdomainMode()))
                .count();
        return new SimpleLoginOverviewVo(
                orgId == null ? null : String.valueOf(orgId),
                aliasCount,
                enabledAliasCount,
                aliasCount - enabledAliasCount,
                mailboxCount,
                verifiedMailboxCount,
                resolveDefaultMailboxEmail(mailboxes),
                reverseAliasContactCount,
                orgScope.domains().size(),
                verifiedCustomDomainCount,
                resolveDefaultDomain(orgScope.domains()),
                relayPolicyCount,
                catchAllDomainCount,
                subdomainPolicyCount,
                resolveDefaultRelayMailboxEmail(orgScope),
                LocalDateTime.now()
        );
    }

    private String resolveDefaultDomain(List<OrgCustomDomain> domains) {
        return domains.stream()
                .filter(domain -> Integer.valueOf(DEFAULT_TRUE).equals(domain.getIsDefault()))
                .map(OrgCustomDomain::getDomain)
                .findFirst()
                .orElse(null);
    }

    private String resolveDefaultRelayMailboxEmail(OrgScope orgScope) {
        String defaultDomain = resolveDefaultDomain(orgScope.domains());
        if (defaultDomain != null) {
            return orgScope.policies().stream()
                    .filter(policy -> matchesDefaultDomain(policy, orgScope.domains(), defaultDomain))
                    .map(SimpleLoginRelayPolicy::getDefaultMailboxEmail)
                    .findFirst()
                    .orElseGet(() -> resolveLatestRelayMailboxEmail(orgScope.policies()));
        }
        return resolveLatestRelayMailboxEmail(orgScope.policies());
    }

    private boolean matchesDefaultDomain(SimpleLoginRelayPolicy policy, List<OrgCustomDomain> domains, String defaultDomain) {
        return domains.stream()
                .anyMatch(domain -> domain.getId().equals(policy.getCustomDomainId()) && defaultDomain.equals(domain.getDomain()));
    }

    private String resolveLatestRelayMailboxEmail(List<SimpleLoginRelayPolicy> policies) {
        return policies.stream()
                .max(Comparator.comparing(SimpleLoginRelayPolicy::getUpdatedAt))
                .map(SimpleLoginRelayPolicy::getDefaultMailboxEmail)
                .orElse(null);
    }

    private long safeCount(Long value) {
        return value == null ? 0L : value;
    }

    private String resolveDefaultMailboxEmail(List<PassMailbox> mailboxes) {
        return mailboxes.stream()
                .filter(mailbox -> Integer.valueOf(DEFAULT_TRUE).equals(mailbox.getIsDefault()))
                .map(PassMailbox::getMailboxEmail)
                .findFirst()
                .or(() -> mailboxes.stream()
                        .max(Comparator.comparing(PassMailbox::getUpdatedAt))
                        .map(PassMailbox::getMailboxEmail))
                .orElse(null);
    }

    private record OrgScope(List<OrgCustomDomain> domains, List<SimpleLoginRelayPolicy> policies) {
    }
}
