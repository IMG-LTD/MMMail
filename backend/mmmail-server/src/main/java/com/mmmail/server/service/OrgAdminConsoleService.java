package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mmmail.server.mapper.OrgCustomDomainMapper;
import com.mmmail.server.mapper.OrgMailIdentityMapper;
import com.mmmail.server.model.entity.OrgCustomDomain;
import com.mmmail.server.model.entity.OrgMailIdentity;
import com.mmmail.server.model.entity.OrgMember;
import com.mmmail.server.model.entity.OrgWorkspace;
import com.mmmail.server.model.vo.OrgAdminConsoleSummaryVo;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrgAdminConsoleService {

    private static final String STATUS_VERIFIED = "VERIFIED";
    private static final String STATUS_ENABLED = "ENABLED";

    private final OrgAccessService orgAccessService;
    private final OrgProductAccessService orgProductAccessService;
    private final OrgCustomDomainMapper orgCustomDomainMapper;
    private final OrgMailIdentityMapper orgMailIdentityMapper;
    private final AuditService auditService;

    public OrgAdminConsoleService(
            OrgAccessService orgAccessService,
            OrgProductAccessService orgProductAccessService,
            OrgCustomDomainMapper orgCustomDomainMapper,
            OrgMailIdentityMapper orgMailIdentityMapper,
            AuditService auditService
    ) {
        this.orgAccessService = orgAccessService;
        this.orgProductAccessService = orgProductAccessService;
        this.orgCustomDomainMapper = orgCustomDomainMapper;
        this.orgMailIdentityMapper = orgMailIdentityMapper;
        this.auditService = auditService;
    }

    public OrgAdminConsoleSummaryVo getSummary(Long userId, Long orgId, String ipAddress) {
        OrgWorkspace org = orgAccessService.loadOrg(orgId);
        OrgMember actor = orgAccessService.requireActiveMember(userId, orgId);
        List<OrgMember> members = orgAccessService.listActiveMembers(orgId);
        List<OrgCustomDomain> domains = orgCustomDomainMapper.selectList(new LambdaQueryWrapper<OrgCustomDomain>()
                .eq(OrgCustomDomain::getOrgId, orgId));
        List<OrgMailIdentity> identities = orgMailIdentityMapper.selectList(new LambdaQueryWrapper<OrgMailIdentity>()
                .eq(OrgMailIdentity::getOrgId, orgId));
        int adminCount = (int) members.stream()
                .filter(member -> OrgAccessService.ROLE_ADMIN.equals(member.getRole()) || OrgAccessService.ROLE_OWNER.equals(member.getRole()))
                .count();
        int verifiedDomainCount = (int) domains.stream().filter(domain -> STATUS_VERIFIED.equals(domain.getStatus())).count();
        int enabledMailIdentityCount = (int) identities.stream().filter(identity -> STATUS_ENABLED.equals(identity.getStatus())).count();
        String defaultDomain = domains.stream()
                .filter(domain -> Integer.valueOf(1).equals(domain.getIsDefault()))
                .map(OrgCustomDomain::getDomain)
                .findFirst()
                .orElse(null);
        String defaultSenderAddress = identities.stream()
                .filter(identity -> Integer.valueOf(1).equals(identity.getIsDefault()))
                .map(OrgMailIdentity::getEmailAddress)
                .findFirst()
                .orElse(null);
        int enabledProductCount = orgProductAccessService.countEnabledProducts(orgId, actor);
        auditService.record(userId, "ORG_ADMIN_SUMMARY_VIEW", "orgId=" + orgId + ",role=" + actor.getRole(), ipAddress, orgId);
        return new OrgAdminConsoleSummaryVo(
                String.valueOf(org.getId()),
                org.getName(),
                actor.getRole(),
                members.size(),
                adminCount,
                domains.size(),
                verifiedDomainCount,
                identities.size(),
                enabledMailIdentityCount,
                enabledProductCount,
                defaultDomain,
                defaultSenderAddress,
                LocalDateTime.now()
        );
    }
}
