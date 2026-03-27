package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.mapper.OrgCustomDomainMapper;
import com.mmmail.server.mapper.PassMailboxMapper;
import com.mmmail.server.mapper.SimpleLoginRelayPolicyMapper;
import com.mmmail.server.model.dto.CreateSimpleLoginRelayPolicyRequest;
import com.mmmail.server.model.dto.UpdateSimpleLoginRelayPolicyRequest;
import com.mmmail.server.model.entity.OrgCustomDomain;
import com.mmmail.server.model.entity.PassMailbox;
import com.mmmail.server.model.entity.SimpleLoginRelayPolicy;
import com.mmmail.server.model.vo.SimpleLoginRelayPolicyVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class SimpleLoginRelayPolicyService {

    public static final String SUBDOMAIN_DISABLED = "DISABLED";
    public static final String SUBDOMAIN_TEAM_PREFIX = "TEAM_PREFIX";
    public static final String SUBDOMAIN_ANY_PREFIX = "ANY_PREFIX";
    private static final String DOMAIN_STATUS_VERIFIED = "VERIFIED";
    private static final String MAILBOX_STATUS_VERIFIED = "VERIFIED";
    private static final int DEFAULT_FALSE = 0;
    private static final int DEFAULT_TRUE = 1;
    private static final Set<String> ALLOWED_SUBDOMAIN_MODES = Set.of(
            SUBDOMAIN_DISABLED,
            SUBDOMAIN_TEAM_PREFIX,
            SUBDOMAIN_ANY_PREFIX
    );

    private final SimpleLoginRelayPolicyMapper simpleLoginRelayPolicyMapper;
    private final OrgCustomDomainMapper orgCustomDomainMapper;
    private final PassMailboxMapper passMailboxMapper;
    private final OrgAccessService orgAccessService;
    private final AuditService auditService;

    public SimpleLoginRelayPolicyService(
            SimpleLoginRelayPolicyMapper simpleLoginRelayPolicyMapper,
            OrgCustomDomainMapper orgCustomDomainMapper,
            PassMailboxMapper passMailboxMapper,
            OrgAccessService orgAccessService,
            AuditService auditService
    ) {
        this.simpleLoginRelayPolicyMapper = simpleLoginRelayPolicyMapper;
        this.orgCustomDomainMapper = orgCustomDomainMapper;
        this.passMailboxMapper = passMailboxMapper;
        this.orgAccessService = orgAccessService;
        this.auditService = auditService;
    }

    public List<SimpleLoginRelayPolicyVo> listPolicies(Long userId, Long orgId, String ipAddress) {
        orgAccessService.requireActiveMember(userId, orgId);
        List<SimpleLoginRelayPolicy> policies = simpleLoginRelayPolicyMapper.selectList(new LambdaQueryWrapper<SimpleLoginRelayPolicy>()
                .eq(SimpleLoginRelayPolicy::getOrgId, orgId)
                .orderByDesc(SimpleLoginRelayPolicy::getUpdatedAt));
        Map<Long, OrgCustomDomain> domainMap = loadDomainMap(orgId);
        auditService.record(userId, "SIMPLELOGIN_POLICY_LIST", "orgId=" + orgId + ",count=" + policies.size(), ipAddress, orgId);
        return policies.stream()
                .map(policy -> toVo(policy, domainMap.get(policy.getCustomDomainId())))
                .toList();
    }

    @Transactional
    public SimpleLoginRelayPolicyVo createPolicy(
            Long userId,
            Long orgId,
            CreateSimpleLoginRelayPolicyRequest request,
            String ipAddress
    ) {
        orgAccessService.requireManageMember(userId, orgId);
        OrgCustomDomain domain = requireVerifiedDomain(orgId, request.customDomainId());
        assertPolicyMissing(domain.getId());
        PassMailbox mailbox = requireVerifiedMailbox(userId, request.defaultMailboxId());
        LocalDateTime now = LocalDateTime.now();
        SimpleLoginRelayPolicy policy = buildPolicy(userId, orgId, domain, mailbox, request.catchAllEnabled(), request.subdomainMode(), request.note(), now);
        simpleLoginRelayPolicyMapper.insert(policy);
        auditService.record(userId, "SIMPLELOGIN_POLICY_CREATE", "orgId=" + orgId + ",domain=" + domain.getDomain(), ipAddress, orgId);
        return toVo(policy, domain);
    }

    @Transactional
    public SimpleLoginRelayPolicyVo updatePolicy(
            Long userId,
            Long orgId,
            Long policyId,
            UpdateSimpleLoginRelayPolicyRequest request,
            String ipAddress
    ) {
        orgAccessService.requireManageMember(userId, orgId);
        SimpleLoginRelayPolicy policy = loadPolicy(orgId, policyId);
        PassMailbox mailbox = requireVerifiedMailbox(userId, request.defaultMailboxId());
        applyPolicyUpdate(policy, mailbox, request.catchAllEnabled(), request.subdomainMode(), request.note());
        simpleLoginRelayPolicyMapper.updateById(policy);
        OrgCustomDomain domain = requireVerifiedDomain(orgId, policy.getCustomDomainId());
        auditService.record(userId, "SIMPLELOGIN_POLICY_UPDATE", "orgId=" + orgId + ",policyId=" + policyId, ipAddress, orgId);
        return toVo(policy, domain);
    }

    @Transactional
    public void deletePolicy(Long userId, Long orgId, Long policyId, String ipAddress) {
        orgAccessService.requireManageMember(userId, orgId);
        SimpleLoginRelayPolicy policy = loadPolicy(orgId, policyId);
        simpleLoginRelayPolicyMapper.deleteById(policy.getId());
        auditService.record(userId, "SIMPLELOGIN_POLICY_DELETE", "orgId=" + orgId + ",policyId=" + policyId, ipAddress, orgId);
    }

    private Map<Long, OrgCustomDomain> loadDomainMap(Long orgId) {
        List<OrgCustomDomain> domains = orgCustomDomainMapper.selectList(new LambdaQueryWrapper<OrgCustomDomain>()
                .eq(OrgCustomDomain::getOrgId, orgId));
        Map<Long, OrgCustomDomain> result = new LinkedHashMap<>();
        for (OrgCustomDomain domain : domains) {
            result.put(domain.getId(), domain);
        }
        return result;
    }

    private OrgCustomDomain requireVerifiedDomain(Long orgId, Long customDomainId) {
        OrgCustomDomain domain = orgCustomDomainMapper.selectOne(new LambdaQueryWrapper<OrgCustomDomain>()
                .eq(OrgCustomDomain::getOrgId, orgId)
                .eq(OrgCustomDomain::getId, customDomainId)
                .last("limit 1"));
        if (domain == null) {
            throw new BizException(ErrorCode.ORG_CUSTOM_DOMAIN_NOT_FOUND, "Custom domain not found");
        }
        if (!DOMAIN_STATUS_VERIFIED.equals(domain.getStatus())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Only verified custom domains can host relay policies");
        }
        return domain;
    }

    private void assertPolicyMissing(Long customDomainId) {
        Long count = simpleLoginRelayPolicyMapper.selectCount(new LambdaQueryWrapper<SimpleLoginRelayPolicy>()
                .eq(SimpleLoginRelayPolicy::getCustomDomainId, customDomainId));
        if (count != null && count > 0) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Relay policy already exists for this domain");
        }
    }

    private PassMailbox requireVerifiedMailbox(Long userId, Long mailboxId) {
        PassMailbox mailbox = passMailboxMapper.selectOne(new LambdaQueryWrapper<PassMailbox>()
                .eq(PassMailbox::getOwnerId, userId)
                .eq(PassMailbox::getId, mailboxId)
                .last("limit 1"));
        if (mailbox == null) {
            throw new BizException(ErrorCode.PASS_MAILBOX_NOT_FOUND, "Pass mailbox not found");
        }
        if (!MAILBOX_STATUS_VERIFIED.equals(mailbox.getStatus())) {
            throw new BizException(ErrorCode.PASS_MAILBOX_NOT_VERIFIED, "Mailbox must be verified first");
        }
        return mailbox;
    }

    private SimpleLoginRelayPolicy loadPolicy(Long orgId, Long policyId) {
        SimpleLoginRelayPolicy policy = simpleLoginRelayPolicyMapper.selectOne(new LambdaQueryWrapper<SimpleLoginRelayPolicy>()
                .eq(SimpleLoginRelayPolicy::getOrgId, orgId)
                .eq(SimpleLoginRelayPolicy::getId, policyId)
                .last("limit 1"));
        if (policy == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Relay policy not found");
        }
        return policy;
    }

    private SimpleLoginRelayPolicy buildPolicy(
            Long userId,
            Long orgId,
            OrgCustomDomain domain,
            PassMailbox mailbox,
            Boolean catchAllEnabled,
            String subdomainMode,
            String note,
            LocalDateTime now
    ) {
        SimpleLoginRelayPolicy policy = new SimpleLoginRelayPolicy();
        policy.setOrgId(orgId);
        policy.setCustomDomainId(domain.getId());
        policy.setOwnerId(userId);
        policy.setCatchAllEnabled(Boolean.TRUE.equals(catchAllEnabled) ? DEFAULT_TRUE : DEFAULT_FALSE);
        policy.setSubdomainMode(normalizeSubdomainMode(subdomainMode));
        policy.setDefaultMailboxId(mailbox.getId());
        policy.setDefaultMailboxEmail(mailbox.getMailboxEmail());
        policy.setNote(normalizeNote(note));
        policy.setCreatedAt(now);
        policy.setUpdatedAt(now);
        policy.setDeleted(DEFAULT_FALSE);
        return policy;
    }

    private void applyPolicyUpdate(
            SimpleLoginRelayPolicy policy,
            PassMailbox mailbox,
            Boolean catchAllEnabled,
            String subdomainMode,
            String note
    ) {
        policy.setCatchAllEnabled(Boolean.TRUE.equals(catchAllEnabled) ? DEFAULT_TRUE : DEFAULT_FALSE);
        policy.setSubdomainMode(normalizeSubdomainMode(subdomainMode));
        policy.setDefaultMailboxId(mailbox.getId());
        policy.setDefaultMailboxEmail(mailbox.getMailboxEmail());
        policy.setNote(normalizeNote(note));
        policy.setUpdatedAt(LocalDateTime.now());
    }

    private String normalizeSubdomainMode(String rawValue) {
        String normalized = rawValue == null ? "" : rawValue.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_SUBDOMAIN_MODES.contains(normalized)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Unsupported subdomain mode");
        }
        return normalized;
    }

    private String normalizeNote(String rawValue) {
        if (!StringUtils.hasText(rawValue)) {
            return null;
        }
        return rawValue.trim();
    }

    private SimpleLoginRelayPolicyVo toVo(SimpleLoginRelayPolicy policy, OrgCustomDomain domain) {
        return new SimpleLoginRelayPolicyVo(
                String.valueOf(policy.getId()),
                String.valueOf(policy.getOrgId()),
                String.valueOf(policy.getCustomDomainId()),
                domain == null ? null : domain.getDomain(),
                Integer.valueOf(DEFAULT_TRUE).equals(policy.getCatchAllEnabled()),
                policy.getSubdomainMode(),
                String.valueOf(policy.getDefaultMailboxId()),
                policy.getDefaultMailboxEmail(),
                policy.getNote(),
                policy.getCreatedAt(),
                policy.getUpdatedAt()
        );
    }
}
