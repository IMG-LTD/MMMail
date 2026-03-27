package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.mapper.OrgCustomDomainMapper;
import com.mmmail.server.mapper.OrgMailIdentityMapper;
import com.mmmail.server.mapper.OrgMemberMapper;
import com.mmmail.server.mapper.OrgWorkspaceMapper;
import com.mmmail.server.mapper.UserAccountMapper;
import com.mmmail.server.model.dto.CreateOrgMailIdentityRequest;
import com.mmmail.server.model.entity.OrgCustomDomain;
import com.mmmail.server.model.entity.OrgMailIdentity;
import com.mmmail.server.model.entity.OrgMember;
import com.mmmail.server.model.entity.OrgWorkspace;
import com.mmmail.server.model.entity.UserAccount;
import com.mmmail.server.model.vo.MailSenderIdentityVo;
import com.mmmail.server.model.vo.OrgMailIdentityVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class OrgMailIdentityService {

    private static final String STATUS_VERIFIED = "VERIFIED";
    private static final String STATUS_ENABLED = "ENABLED";
    private static final String STATUS_DISABLED = "DISABLED";
    private static final String SOURCE_PRIMARY = "PRIMARY";
    private static final String SOURCE_ORG_CUSTOM_DOMAIN = "ORG_CUSTOM_DOMAIN";
    private static final int DEFAULT_TRUE = 1;
    private static final int DEFAULT_FALSE = 0;
    private static final Pattern LOCAL_PART_PATTERN = Pattern.compile("^[a-z0-9](?:[a-z0-9._+-]{0,62}[a-z0-9])?$");

    private final OrgAccessService orgAccessService;
    private final OrgCustomDomainMapper orgCustomDomainMapper;
    private final OrgMailIdentityMapper orgMailIdentityMapper;
    private final OrgMemberMapper orgMemberMapper;
    private final OrgWorkspaceMapper orgWorkspaceMapper;
    private final UserAccountMapper userAccountMapper;
    private final AuditService auditService;

    public OrgMailIdentityService(
            OrgAccessService orgAccessService,
            OrgCustomDomainMapper orgCustomDomainMapper,
            OrgMailIdentityMapper orgMailIdentityMapper,
            OrgMemberMapper orgMemberMapper,
            OrgWorkspaceMapper orgWorkspaceMapper,
            UserAccountMapper userAccountMapper,
            AuditService auditService
    ) {
        this.orgAccessService = orgAccessService;
        this.orgCustomDomainMapper = orgCustomDomainMapper;
        this.orgMailIdentityMapper = orgMailIdentityMapper;
        this.orgMemberMapper = orgMemberMapper;
        this.orgWorkspaceMapper = orgWorkspaceMapper;
        this.userAccountMapper = userAccountMapper;
        this.auditService = auditService;
    }

    public List<OrgMailIdentityVo> listOrgIdentities(Long userId, Long orgId, String ipAddress) {
        OrgMember actor = orgAccessService.requireActiveMember(userId, orgId);
        LambdaQueryWrapper<OrgMailIdentity> query = new LambdaQueryWrapper<OrgMailIdentity>()
                .eq(OrgMailIdentity::getOrgId, orgId)
                .orderByDesc(OrgMailIdentity::getIsDefault)
                .orderByAsc(OrgMailIdentity::getStatus)
                .orderByDesc(OrgMailIdentity::getUpdatedAt);
        if (!orgAccessService.canManage(actor.getRole())) {
            query.eq(OrgMailIdentity::getMemberId, actor.getId());
        }
        Map<Long, OrgMember> memberMap = loadMemberMap(orgId);
        auditService.record(userId, "ORG_MAIL_IDENTITY_LIST", "orgId=" + orgId + ",role=" + actor.getRole(), ipAddress, orgId);
        return orgMailIdentityMapper.selectList(query).stream()
                .map(identity -> toOrgIdentityVo(identity, memberMap.get(identity.getMemberId())))
                .toList();
    }

    @Transactional
    public OrgMailIdentityVo createIdentity(Long userId, Long orgId, CreateOrgMailIdentityRequest request, String ipAddress) {
        orgAccessService.requireManageMember(userId, orgId);
        OrgMember member = orgAccessService.loadActiveMemberById(orgId, request.memberId());
        OrgCustomDomain domain = loadVerifiedDomain(orgId, request.customDomainId());
        String localPart = normalizeLocalPart(request.localPart());
        String emailAddress = buildEmailAddress(localPart, domain.getDomain());
        assertUniqueIdentity(orgId, emailAddress);
        LocalDateTime now = LocalDateTime.now();
        OrgMailIdentity identity = new OrgMailIdentity();
        identity.setOrgId(orgId);
        identity.setMemberId(member.getId());
        identity.setCustomDomainId(domain.getId());
        identity.setLocalPart(localPart);
        identity.setEmailAddress(emailAddress);
        identity.setDisplayName(normalizeDisplayName(request.displayName()));
        identity.setStatus(STATUS_ENABLED);
        identity.setIsDefault(hasDefaultIdentity(member.getUserId()) ? DEFAULT_FALSE : DEFAULT_TRUE);
        identity.setCreatedBy(userId);
        identity.setUpdatedBy(userId);
        identity.setCreatedAt(now);
        identity.setUpdatedAt(now);
        identity.setDeleted(DEFAULT_FALSE);
        orgMailIdentityMapper.insert(identity);
        auditService.record(userId, "ORG_MAIL_IDENTITY_CREATE", "orgId=" + orgId + ",memberId=" + member.getId() + ",email=" + emailAddress, ipAddress, orgId);
        return toOrgIdentityVo(identity, member);
    }

    @Transactional
    public OrgMailIdentityVo enableIdentity(Long userId, Long orgId, Long identityId, String ipAddress) {
        orgAccessService.requireManageMember(userId, orgId);
        OrgMailIdentity identity = loadIdentity(orgId, identityId);
        if (STATUS_ENABLED.equals(identity.getStatus())) {
            return toOrgIdentityVo(identity, loadMember(identity.getMemberId(), orgId));
        }
        updateStatus(identity, STATUS_ENABLED, userId);
        auditService.record(userId, "ORG_MAIL_IDENTITY_ENABLE", "orgId=" + orgId + ",identityId=" + identityId + ",email=" + identity.getEmailAddress(), ipAddress, orgId);
        return toOrgIdentityVo(identity, loadMember(identity.getMemberId(), orgId));
    }

    @Transactional
    public OrgMailIdentityVo disableIdentity(Long userId, Long orgId, Long identityId, String ipAddress) {
        orgAccessService.requireManageMember(userId, orgId);
        OrgMailIdentity identity = loadIdentity(orgId, identityId);
        if (STATUS_DISABLED.equals(identity.getStatus())) {
            return toOrgIdentityVo(identity, loadMember(identity.getMemberId(), orgId));
        }
        identity.setStatus(STATUS_DISABLED);
        identity.setIsDefault(DEFAULT_FALSE);
        identity.setUpdatedBy(userId);
        identity.setUpdatedAt(LocalDateTime.now());
        orgMailIdentityMapper.updateById(identity);
        auditService.record(userId, "ORG_MAIL_IDENTITY_DISABLE", "orgId=" + orgId + ",identityId=" + identityId + ",email=" + identity.getEmailAddress(), ipAddress, orgId);
        return toOrgIdentityVo(identity, loadMember(identity.getMemberId(), orgId));
    }

    @Transactional
    public OrgMailIdentityVo setDefaultIdentity(Long userId, Long orgId, Long identityId, String ipAddress) {
        orgAccessService.requireManageMember(userId, orgId);
        OrgMailIdentity identity = loadIdentity(orgId, identityId);
        if (!STATUS_ENABLED.equals(identity.getStatus())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Only enabled identities can be default senders");
        }
        OrgMember member = loadMember(identity.getMemberId(), orgId);
        clearDefaultIdentityFlags(member.getUserId());
        identity.setIsDefault(DEFAULT_TRUE);
        identity.setUpdatedBy(userId);
        identity.setUpdatedAt(LocalDateTime.now());
        orgMailIdentityMapper.updateById(identity);
        auditService.record(userId, "ORG_MAIL_IDENTITY_SET_DEFAULT", "orgId=" + orgId + ",identityId=" + identityId + ",email=" + identity.getEmailAddress(), ipAddress, orgId);
        return toOrgIdentityVo(identity, member);
    }

    @Transactional
    public void removeIdentity(Long userId, Long orgId, Long identityId, String ipAddress) {
        orgAccessService.requireManageMember(userId, orgId);
        OrgMailIdentity identity = loadIdentity(orgId, identityId);
        orgMailIdentityMapper.deleteById(identity.getId());
        auditService.record(userId, "ORG_MAIL_IDENTITY_DELETE", "orgId=" + orgId + ",identityId=" + identityId + ",email=" + identity.getEmailAddress(), ipAddress, orgId);
    }

    public List<MailSenderIdentityVo> listSenderIdentities(Long userId, String ipAddress) {
        UserAccount user = loadUser(userId);
        List<OrgMember> memberships = loadActiveMemberships(userId);
        Map<Long, OrgMember> memberMap = memberships.stream().collect(Collectors.toMap(OrgMember::getId, member -> member));
        Map<Long, String> orgNameMap = loadOrgNameMap(memberships);
        List<OrgMailIdentity> enabledIdentities = loadEnabledIdentities(memberMap.keySet());
        boolean hasDefaultIdentity = enabledIdentities.stream().anyMatch(this::isDefaultIdentity);
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
        enabledIdentities.stream()
                .sorted(Comparator.comparing(OrgMailIdentity::getIsDefault).reversed().thenComparing(OrgMailIdentity::getUpdatedAt, Comparator.reverseOrder()))
                .map(identity -> toSenderIdentityVo(identity, memberMap.get(identity.getMemberId()), orgNameMap.get(identity.getOrgId())))
                .forEach(options::add);
        List<MailSenderIdentityVo> sortedOptions = options.stream()
                .sorted(Comparator.comparing(MailSenderIdentityVo::defaultIdentity).reversed()
                        .thenComparing(option -> SOURCE_PRIMARY.equals(option.source()) ? 0 : 1)
                        .thenComparing(MailSenderIdentityVo::emailAddress))
                .toList();
        auditService.record(userId, "MAIL_IDENTITY_LIST", "count=" + sortedOptions.size(), ipAddress);
        return sortedOptions;
    }

    public String resolveAuthorizedSenderEmail(Long userId, String fromEmail) {
        UserAccount user = loadUser(userId);
        if (!StringUtils.hasText(fromEmail)) {
            return user.getEmail();
        }
        String normalizedEmail = fromEmail.trim().toLowerCase(Locale.ROOT);
        if (normalizedEmail.equals(user.getEmail().trim().toLowerCase(Locale.ROOT))) {
            return user.getEmail();
        }
        String orgSender = resolveAuthorizedOrgSenderEmailOrNull(userId, normalizedEmail);
        if (orgSender == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Sender identity is unavailable");
        }
        return orgSender;
    }

    public List<MailSenderIdentityVo> listEnabledOrgSenderIdentities(Long userId) {
        List<OrgMember> memberships = loadActiveMemberships(userId);
        if (memberships.isEmpty()) {
            return List.of();
        }
        Map<Long, OrgMember> memberMap = memberships.stream().collect(Collectors.toMap(OrgMember::getId, member -> member));
        Map<Long, String> orgNameMap = loadOrgNameMap(memberships);
        return loadEnabledIdentities(memberMap.keySet()).stream()
                .sorted(Comparator.comparing(OrgMailIdentity::getIsDefault).reversed()
                        .thenComparing(OrgMailIdentity::getUpdatedAt, Comparator.reverseOrder()))
                .map(identity -> toSenderIdentityVo(identity, memberMap.get(identity.getMemberId()), orgNameMap.get(identity.getOrgId())))
                .toList();
    }

    public Set<String> listEnabledOrgSenderEmails(Long userId) {
        Set<Long> memberIds = loadActiveMemberships(userId).stream().map(OrgMember::getId).collect(Collectors.toSet());
        if (memberIds.isEmpty()) {
            return Set.of();
        }
        return loadEnabledIdentities(memberIds).stream()
                .map(OrgMailIdentity::getEmailAddress)
                .collect(Collectors.toSet());
    }

    public String resolveAuthorizedOrgSenderEmailOrNull(Long userId, String normalizedEmail) {
        Set<Long> memberIds = loadActiveMemberships(userId).stream().map(OrgMember::getId).collect(Collectors.toSet());
        if (memberIds.isEmpty()) {
            return null;
        }
        OrgMailIdentity identity = orgMailIdentityMapper.selectOne(new LambdaQueryWrapper<OrgMailIdentity>()
                .in(OrgMailIdentity::getMemberId, memberIds)
                .eq(OrgMailIdentity::getEmailAddress, normalizedEmail)
                .eq(OrgMailIdentity::getStatus, STATUS_ENABLED)
                .last("limit 1"));
        return identity == null ? null : identity.getEmailAddress();
    }

    public boolean hasActiveIdentitiesForDomain(Long orgId, Long domainId) {
        return orgMailIdentityMapper.selectCount(new LambdaQueryWrapper<OrgMailIdentity>()
                .eq(OrgMailIdentity::getOrgId, orgId)
                .eq(OrgMailIdentity::getCustomDomainId, domainId)) > 0;
    }

    private OrgCustomDomain loadVerifiedDomain(Long orgId, Long domainId) {
        OrgCustomDomain domain = orgCustomDomainMapper.selectOne(new LambdaQueryWrapper<OrgCustomDomain>()
                .eq(OrgCustomDomain::getOrgId, orgId)
                .eq(OrgCustomDomain::getId, domainId)
                .last("limit 1"));
        if (domain == null) {
            throw new BizException(ErrorCode.ORG_CUSTOM_DOMAIN_NOT_FOUND, "Custom domain not found");
        }
        if (!STATUS_VERIFIED.equals(domain.getStatus())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Only verified custom domains can host sender identities");
        }
        return domain;
    }

    private OrgMailIdentity loadIdentity(Long orgId, Long identityId) {
        OrgMailIdentity identity = orgMailIdentityMapper.selectOne(new LambdaQueryWrapper<OrgMailIdentity>()
                .eq(OrgMailIdentity::getOrgId, orgId)
                .eq(OrgMailIdentity::getId, identityId)
                .last("limit 1"));
        if (identity == null) {
            throw new BizException(ErrorCode.ORG_MAIL_IDENTITY_NOT_FOUND, "Mail identity not found");
        }
        return identity;
    }

    private OrgMember loadMember(Long memberId, Long orgId) {
        OrgMember member = orgAccessService.loadActiveMemberById(orgId, memberId);
        if (member == null) {
            throw new BizException(ErrorCode.ORG_MEMBER_NOT_FOUND, "Organization member not found");
        }
        return member;
    }

    private Map<Long, OrgMember> loadMemberMap(Long orgId) {
        return orgAccessService.listActiveMembers(orgId).stream()
                .collect(Collectors.toMap(OrgMember::getId, member -> member));
    }

    private List<OrgMember> loadActiveMemberships(Long userId) {
        return orgMemberMapper.selectList(new LambdaQueryWrapper<OrgMember>()
                .eq(OrgMember::getUserId, userId)
                .eq(OrgMember::getStatus, OrgAccessService.STATUS_ACTIVE)
                .orderByDesc(OrgMember::getUpdatedAt));
    }

    private Map<Long, String> loadOrgNameMap(List<OrgMember> memberships) {
        Set<Long> orgIds = memberships.stream().map(OrgMember::getOrgId).collect(Collectors.toSet());
        if (orgIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> names = new LinkedHashMap<>();
        List<OrgWorkspace> workspaces = orgWorkspaceMapper.selectBatchIds(orgIds);
        for (OrgWorkspace workspace : workspaces) {
            names.put(workspace.getId(), workspace.getName());
        }
        return names;
    }

    private List<OrgMailIdentity> loadEnabledIdentities(Set<Long> memberIds) {
        if (memberIds.isEmpty()) {
            return List.of();
        }
        return orgMailIdentityMapper.selectList(new LambdaQueryWrapper<OrgMailIdentity>()
                .in(OrgMailIdentity::getMemberId, memberIds)
                .eq(OrgMailIdentity::getStatus, STATUS_ENABLED));
    }

    private void assertUniqueIdentity(Long orgId, String emailAddress) {
        OrgMailIdentity existing = orgMailIdentityMapper.selectOne(new LambdaQueryWrapper<OrgMailIdentity>()
                .eq(OrgMailIdentity::getOrgId, orgId)
                .eq(OrgMailIdentity::getEmailAddress, emailAddress)
                .last("limit 1"));
        if (existing != null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Mail identity already exists in this organization");
        }
    }

    private boolean hasDefaultIdentity(Long userId) {
        Set<Long> memberIds = loadActiveMemberships(userId).stream().map(OrgMember::getId).collect(Collectors.toSet());
        if (memberIds.isEmpty()) {
            return false;
        }
        return orgMailIdentityMapper.selectCount(new LambdaQueryWrapper<OrgMailIdentity>()
                .in(OrgMailIdentity::getMemberId, memberIds)
                .eq(OrgMailIdentity::getStatus, STATUS_ENABLED)
                .eq(OrgMailIdentity::getIsDefault, DEFAULT_TRUE)) > 0;
    }

    private void clearDefaultIdentityFlags(Long userId) {
        Set<Long> memberIds = loadActiveMemberships(userId).stream().map(OrgMember::getId).collect(Collectors.toSet());
        if (memberIds.isEmpty()) {
            return;
        }
        List<OrgMailIdentity> identities = orgMailIdentityMapper.selectList(new LambdaQueryWrapper<OrgMailIdentity>()
                .in(OrgMailIdentity::getMemberId, memberIds)
                .eq(OrgMailIdentity::getIsDefault, DEFAULT_TRUE));
        for (OrgMailIdentity identity : identities) {
            identity.setIsDefault(DEFAULT_FALSE);
            identity.setUpdatedAt(LocalDateTime.now());
            orgMailIdentityMapper.updateById(identity);
        }
    }

    private void updateStatus(OrgMailIdentity identity, String status, Long actorId) {
        identity.setStatus(status);
        identity.setUpdatedBy(actorId);
        identity.setUpdatedAt(LocalDateTime.now());
        orgMailIdentityMapper.updateById(identity);
    }

    private boolean isDefaultIdentity(OrgMailIdentity identity) {
        return Integer.valueOf(DEFAULT_TRUE).equals(identity.getIsDefault());
    }

    private UserAccount loadUser(Long userId) {
        UserAccount user = userAccountMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND, "User not found");
        }
        return user;
    }

    private String normalizeLocalPart(String localPart) {
        if (!StringUtils.hasText(localPart)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Identity local part is required");
        }
        String normalized = localPart.trim().toLowerCase(Locale.ROOT);
        if (!LOCAL_PART_PATTERN.matcher(normalized).matches()) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Identity local part format is invalid");
        }
        return normalized;
    }

    private String normalizeDisplayName(String displayName) {
        if (!StringUtils.hasText(displayName)) {
            return null;
        }
        return displayName.trim();
    }

    private String buildEmailAddress(String localPart, String domain) {
        return localPart + "@" + domain.trim().toLowerCase(Locale.ROOT);
    }

    private OrgMailIdentityVo toOrgIdentityVo(OrgMailIdentity identity, OrgMember member) {
        return new OrgMailIdentityVo(
                String.valueOf(identity.getId()),
                String.valueOf(identity.getOrgId()),
                String.valueOf(identity.getMemberId()),
                member == null ? null : member.getUserEmail(),
                String.valueOf(identity.getCustomDomainId()),
                identity.getLocalPart(),
                identity.getEmailAddress(),
                identity.getDisplayName(),
                identity.getStatus(),
                isDefaultIdentity(identity),
                identity.getCreatedBy() == null ? null : String.valueOf(identity.getCreatedBy()),
                identity.getUpdatedAt()
        );
    }

    private MailSenderIdentityVo toSenderIdentityVo(OrgMailIdentity identity, OrgMember member, String orgName) {
        return new MailSenderIdentityVo(
                String.valueOf(identity.getId()),
                String.valueOf(identity.getOrgId()),
                orgName,
                String.valueOf(identity.getMemberId()),
                identity.getEmailAddress(),
                StringUtils.hasText(identity.getDisplayName()) ? identity.getDisplayName() : (member == null ? null : member.getUserEmail()),
                SOURCE_ORG_CUSTOM_DOMAIN,
                identity.getStatus(),
                isDefaultIdentity(identity)
        );
    }
}
