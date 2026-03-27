package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.mapper.OrgMemberMapper;
import com.mmmail.server.mapper.OrgTeamSpaceMapper;
import com.mmmail.server.mapper.OrgTeamSpaceMemberMapper;
import com.mmmail.server.mapper.OrgWorkspaceMapper;
import com.mmmail.server.model.dto.CreateOrgTeamSpaceMemberRequest;
import com.mmmail.server.model.dto.UpdateOrgTeamSpaceMemberRoleRequest;
import com.mmmail.server.model.entity.OrgMember;
import com.mmmail.server.model.entity.OrgTeamSpace;
import com.mmmail.server.model.entity.OrgTeamSpaceMember;
import com.mmmail.server.model.vo.OrgAuditEventVo;
import com.mmmail.server.model.vo.OrgTeamSpaceActivityVo;
import com.mmmail.server.model.vo.OrgTeamSpaceMemberVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class OrgTeamSpaceGovernanceService {

    private static final String ROLE_OWNER = "OWNER";
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_MANAGER = "MANAGER";
    private static final String ROLE_EDITOR = "EDITOR";
    private static final String ROLE_VIEWER = "VIEWER";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 200;
    private static final String TEAM_SPACE_TOKEN = "teamSpaceId=";
    private static final Set<String> TEAM_SPACE_EVENT_TYPES = Set.of(
            "ORG_TEAM_SPACE_MEMBER_ADD",
            "ORG_TEAM_SPACE_MEMBER_ROLE_UPDATE",
            "ORG_TEAM_SPACE_MEMBER_REMOVE",
            "ORG_TEAM_SPACE_FOLDER_CREATE",
            "ORG_TEAM_SPACE_FILE_UPLOAD",
            "ORG_TEAM_SPACE_FILE_DOWNLOAD",
            "ORG_TEAM_SPACE_FILE_VERSION_UPLOAD",
            "ORG_TEAM_SPACE_FILE_VERSION_RESTORE",
            "ORG_TEAM_SPACE_ITEM_DELETE",
            "ORG_TEAM_SPACE_ITEM_RESTORE",
            "ORG_TEAM_SPACE_ITEM_PURGE"
    );

    private final OrgWorkspaceMapper orgWorkspaceMapper;
    private final OrgMemberMapper orgMemberMapper;
    private final OrgTeamSpaceMapper orgTeamSpaceMapper;
    private final OrgTeamSpaceMemberMapper orgTeamSpaceMemberMapper;
    private final AuditService auditService;

    public OrgTeamSpaceGovernanceService(
            OrgWorkspaceMapper orgWorkspaceMapper,
            OrgMemberMapper orgMemberMapper,
            OrgTeamSpaceMapper orgTeamSpaceMapper,
            OrgTeamSpaceMemberMapper orgTeamSpaceMemberMapper,
            AuditService auditService
    ) {
        this.orgWorkspaceMapper = orgWorkspaceMapper;
        this.orgMemberMapper = orgMemberMapper;
        this.orgTeamSpaceMapper = orgTeamSpaceMapper;
        this.orgTeamSpaceMemberMapper = orgTeamSpaceMemberMapper;
        this.auditService = auditService;
    }

    public TeamSpaceAccessScope describeAccess(Long userId, Long orgId, Long teamSpaceId) {
        OrgMember orgMember = requireActiveOrgMember(userId, orgId);
        loadTeamSpace(orgId, teamSpaceId);
        OrgTeamSpaceMember membership = findTeamSpaceMember(teamSpaceId, userId);
        if (membership != null) {
            String role = membership.getRole();
            return new TeamSpaceAccessScope(role, true, canWrite(role), canManage(role));
        }
        if (isBootstrapManager(orgMember, teamSpaceId)) {
            return new TeamSpaceAccessScope(ROLE_MANAGER, true, true, true);
        }
        return new TeamSpaceAccessScope(null, false, false, false);
    }

    @Transactional
    public void ensureCreatorManager(Long orgId, Long teamSpaceId, Long userId) {
        OrgMember orgMember = requireActiveOrgMember(userId, orgId);
        if (findTeamSpaceMember(teamSpaceId, userId) != null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        OrgTeamSpaceMember member = new OrgTeamSpaceMember();
        member.setOrgId(orgId);
        member.setTeamSpaceId(teamSpaceId);
        member.setUserId(userId);
        member.setUserEmail(orgMember.getUserEmail());
        member.setRole(ROLE_MANAGER);
        member.setCreatedBy(userId);
        member.setCreatedAt(now);
        member.setUpdatedAt(now);
        member.setDeleted(0);
        orgTeamSpaceMemberMapper.insert(member);
    }

    public List<OrgTeamSpaceMemberVo> listMembers(Long userId, Long orgId, Long teamSpaceId, String ipAddress) {
        requireManagerAccess(userId, orgId, teamSpaceId);
        List<OrgTeamSpaceMember> members = orgTeamSpaceMemberMapper.selectList(new LambdaQueryWrapper<OrgTeamSpaceMember>()
                .eq(OrgTeamSpaceMember::getOrgId, orgId)
                .eq(OrgTeamSpaceMember::getTeamSpaceId, teamSpaceId)
                .orderByAsc(OrgTeamSpaceMember::getRole)
                .orderByAsc(OrgTeamSpaceMember::getUserEmail));
        auditService.record(userId, "ORG_TEAM_SPACE_MEMBER_LIST", detail(teamSpaceId, "memberCount=" + members.size()), ipAddress, orgId);
        return members.stream().map(item -> toMemberVo(item, userId)).toList();
    }

    @Transactional
    public OrgTeamSpaceMemberVo addMember(
            Long userId,
            Long orgId,
            Long teamSpaceId,
            CreateOrgTeamSpaceMemberRequest request,
            String ipAddress
    ) {
        requireManagerAccess(userId, orgId, teamSpaceId);
        OrgMember orgMember = findActiveOrgMemberByEmail(orgId, request.userEmail());
        if (orgMember == null || orgMember.getUserId() == null) {
            throw new BizException(ErrorCode.ORG_MEMBER_NOT_FOUND, "Organization active member is not found");
        }
        if (findTeamSpaceMember(teamSpaceId, orgMember.getUserId()) != null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Team space member already exists");
        }
        LocalDateTime now = LocalDateTime.now();
        OrgTeamSpaceMember member = new OrgTeamSpaceMember();
        member.setOrgId(orgId);
        member.setTeamSpaceId(teamSpaceId);
        member.setUserId(orgMember.getUserId());
        member.setUserEmail(orgMember.getUserEmail());
        member.setRole(normalizeTeamRole(request.role()));
        member.setCreatedBy(userId);
        member.setCreatedAt(now);
        member.setUpdatedAt(now);
        member.setDeleted(0);
        orgTeamSpaceMemberMapper.insert(member);
        auditService.record(userId, "ORG_TEAM_SPACE_MEMBER_ADD", detail(teamSpaceId, "memberEmail=" + member.getUserEmail() + ",role=" + member.getRole()), ipAddress, orgId);
        return toMemberVo(member, userId);
    }

    @Transactional
    public OrgTeamSpaceMemberVo updateMemberRole(
            Long userId,
            Long orgId,
            Long teamSpaceId,
            Long memberId,
            UpdateOrgTeamSpaceMemberRoleRequest request,
            String ipAddress
    ) {
        requireManagerAccess(userId, orgId, teamSpaceId);
        OrgTeamSpaceMember member = loadTeamSpaceMember(orgId, teamSpaceId, memberId);
        member.setRole(normalizeTeamRole(request.role()));
        member.setUpdatedAt(LocalDateTime.now());
        orgTeamSpaceMemberMapper.updateById(member);
        auditService.record(userId, "ORG_TEAM_SPACE_MEMBER_ROLE_UPDATE", detail(teamSpaceId, "memberEmail=" + member.getUserEmail() + ",role=" + member.getRole()), ipAddress, orgId);
        return toMemberVo(member, userId);
    }

    @Transactional
    public void removeMember(Long userId, Long orgId, Long teamSpaceId, Long memberId, String ipAddress) {
        requireManagerAccess(userId, orgId, teamSpaceId);
        OrgTeamSpaceMember member = loadTeamSpaceMember(orgId, teamSpaceId, memberId);
        orgTeamSpaceMemberMapper.deleteById(memberId);
        auditService.record(userId, "ORG_TEAM_SPACE_MEMBER_REMOVE", detail(teamSpaceId, "memberEmail=" + member.getUserEmail()), ipAddress, orgId);
    }

    public List<OrgTeamSpaceActivityVo> listActivities(
            Long userId,
            Long orgId,
            Long teamSpaceId,
            String category,
            Integer limit
    ) {
        requireReadAccess(userId, orgId, teamSpaceId);
        int safeLimit = normalizeLimit(limit);
        Set<String> categoryEvents = resolveActivityCategoryEvents(category);
        List<OrgAuditEventVo> orgEvents = auditService.listByOrg(orgId, safeLimit * 4, null, null, TEAM_SPACE_TOKEN + teamSpaceId);
        return orgEvents.stream()
                .filter(event -> TEAM_SPACE_EVENT_TYPES.contains(event.eventType()))
                .filter(event -> categoryEvents == null || categoryEvents.contains(event.eventType()))
                .filter(event -> StringUtils.hasText(event.detail()) && event.detail().contains(TEAM_SPACE_TOKEN + teamSpaceId))
                .limit(safeLimit)
                .map(event -> new OrgTeamSpaceActivityVo(event.id(), String.valueOf(teamSpaceId), event.actorEmail(), event.eventType(), event.detail(), event.createdAt()))
                .toList();
    }

    public void requireReadAccess(Long userId, Long orgId, Long teamSpaceId) {
        if (!describeAccess(userId, orgId, teamSpaceId).canRead()) {
            throw new BizException(ErrorCode.ORG_FORBIDDEN, "No Team Space access");
        }
    }

    public void requireWriteAccess(Long userId, Long orgId, Long teamSpaceId) {
        if (!describeAccess(userId, orgId, teamSpaceId).canWrite()) {
            throw new BizException(ErrorCode.ORG_FORBIDDEN, "No Team Space write access");
        }
    }

    public void requireManagerAccess(Long userId, Long orgId, Long teamSpaceId) {
        if (!describeAccess(userId, orgId, teamSpaceId).canManage()) {
            throw new BizException(ErrorCode.ORG_FORBIDDEN, "No Team Space manager access");
        }
    }

    private OrgMember requireActiveOrgMember(Long userId, Long orgId) {
        if (orgWorkspaceMapper.selectById(orgId) == null) {
            throw new BizException(ErrorCode.ORG_NOT_FOUND);
        }
        OrgMember member = orgMemberMapper.selectOne(new LambdaQueryWrapper<OrgMember>()
                .eq(OrgMember::getOrgId, orgId)
                .eq(OrgMember::getUserId, userId)
                .eq(OrgMember::getStatus, STATUS_ACTIVE)
                .last("limit 1"));
        if (member == null) {
            throw new BizException(ErrorCode.ORG_FORBIDDEN, "No active organization access");
        }
        return member;
    }

    private OrgMember findActiveOrgMemberByEmail(Long orgId, String userEmail) {
        return orgMemberMapper.selectOne(new LambdaQueryWrapper<OrgMember>()
                .eq(OrgMember::getOrgId, orgId)
                .eq(OrgMember::getUserEmail, userEmail.trim().toLowerCase(Locale.ROOT))
                .eq(OrgMember::getStatus, STATUS_ACTIVE)
                .last("limit 1"));
    }

    private OrgTeamSpace loadTeamSpace(Long orgId, Long teamSpaceId) {
        OrgTeamSpace teamSpace = orgTeamSpaceMapper.selectOne(new LambdaQueryWrapper<OrgTeamSpace>()
                .eq(OrgTeamSpace::getId, teamSpaceId)
                .eq(OrgTeamSpace::getOrgId, orgId)
                .last("limit 1"));
        if (teamSpace == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Team space is not found");
        }
        return teamSpace;
    }

    private OrgTeamSpaceMember loadTeamSpaceMember(Long orgId, Long teamSpaceId, Long memberId) {
        OrgTeamSpaceMember member = orgTeamSpaceMemberMapper.selectOne(new LambdaQueryWrapper<OrgTeamSpaceMember>()
                .eq(OrgTeamSpaceMember::getId, memberId)
                .eq(OrgTeamSpaceMember::getOrgId, orgId)
                .eq(OrgTeamSpaceMember::getTeamSpaceId, teamSpaceId)
                .last("limit 1"));
        if (member == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Team space member is not found");
        }
        return member;
    }

    private OrgTeamSpaceMember findTeamSpaceMember(Long teamSpaceId, Long userId) {
        return orgTeamSpaceMemberMapper.selectOne(new LambdaQueryWrapper<OrgTeamSpaceMember>()
                .eq(OrgTeamSpaceMember::getTeamSpaceId, teamSpaceId)
                .eq(OrgTeamSpaceMember::getUserId, userId)
                .last("limit 1"));
    }

    private boolean isBootstrapManager(OrgMember orgMember, Long teamSpaceId) {
        if (orgMember == null || orgMember.getRole() == null || hasExplicitMembers(teamSpaceId)) {
            return false;
        }
        return ROLE_OWNER.equals(orgMember.getRole()) || ROLE_ADMIN.equals(orgMember.getRole());
    }

    private boolean hasExplicitMembers(Long teamSpaceId) {
        return orgTeamSpaceMemberMapper.selectCount(new LambdaQueryWrapper<OrgTeamSpaceMember>()
                .eq(OrgTeamSpaceMember::getTeamSpaceId, teamSpaceId)) > 0;
    }

    private boolean canWrite(String role) {
        return ROLE_MANAGER.equals(role) || ROLE_EDITOR.equals(role);
    }

    private boolean canManage(String role) {
        return ROLE_MANAGER.equals(role);
    }

    private int normalizeLimit(Integer limit) {
        int value = limit == null ? DEFAULT_LIMIT : limit;
        return Math.max(1, Math.min(value, MAX_LIMIT));
    }

    private String normalizeTeamRole(String raw) {
        String value = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
        if (!ROLE_MANAGER.equals(value) && !ROLE_EDITOR.equals(value) && !ROLE_VIEWER.equals(value)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Unsupported Team Space role");
        }
        return value;
    }

    private Set<String> resolveActivityCategoryEvents(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        return switch (raw.trim().toUpperCase(Locale.ROOT)) {
            case "MEMBER" -> Set.of("ORG_TEAM_SPACE_MEMBER_ADD", "ORG_TEAM_SPACE_MEMBER_ROLE_UPDATE", "ORG_TEAM_SPACE_MEMBER_REMOVE");
            case "FILE" -> Set.of("ORG_TEAM_SPACE_FOLDER_CREATE", "ORG_TEAM_SPACE_FILE_UPLOAD", "ORG_TEAM_SPACE_FILE_DOWNLOAD");
            case "VERSION" -> Set.of("ORG_TEAM_SPACE_FILE_VERSION_UPLOAD", "ORG_TEAM_SPACE_FILE_VERSION_RESTORE");
            case "TRASH" -> Set.of("ORG_TEAM_SPACE_ITEM_DELETE", "ORG_TEAM_SPACE_ITEM_RESTORE", "ORG_TEAM_SPACE_ITEM_PURGE");
            default -> throw new BizException(ErrorCode.INVALID_ARGUMENT, "Unsupported Team Space activity category");
        };
    }

    private OrgTeamSpaceMemberVo toMemberVo(OrgTeamSpaceMember member, Long currentUserId) {
        return new OrgTeamSpaceMemberVo(
                String.valueOf(member.getId()),
                String.valueOf(member.getOrgId()),
                String.valueOf(member.getTeamSpaceId()),
                String.valueOf(member.getUserId()),
                member.getUserEmail(),
                member.getRole(),
                member.getUserId() != null && member.getUserId().equals(currentUserId),
                member.getUpdatedAt()
        );
    }

    private String detail(Long teamSpaceId, String suffix) {
        return TEAM_SPACE_TOKEN + teamSpaceId + "," + suffix;
    }

    public record TeamSpaceAccessScope(
            String role,
            boolean canRead,
            boolean canWrite,
            boolean canManage
    ) {
    }
}
