package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.mapper.OrgMemberMapper;
import com.mmmail.server.mapper.OrgWorkspaceMapper;
import com.mmmail.server.model.entity.OrgMember;
import com.mmmail.server.model.entity.OrgWorkspace;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrgAccessService {

    public static final String ROLE_OWNER = "OWNER";
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_MEMBER = "MEMBER";
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_DISABLED = "DISABLED";

    private final OrgWorkspaceMapper orgWorkspaceMapper;
    private final OrgMemberMapper orgMemberMapper;

    public OrgAccessService(OrgWorkspaceMapper orgWorkspaceMapper, OrgMemberMapper orgMemberMapper) {
        this.orgWorkspaceMapper = orgWorkspaceMapper;
        this.orgMemberMapper = orgMemberMapper;
    }

    public OrgWorkspace loadOrg(Long orgId) {
        OrgWorkspace org = orgWorkspaceMapper.selectById(orgId);
        if (org == null || org.getDeleted() != null && org.getDeleted() == 1) {
            throw new BizException(ErrorCode.ORG_NOT_FOUND, "Organization not found");
        }
        return org;
    }

    public OrgMember requireActiveMember(Long userId, Long orgId) {
        loadOrg(orgId);
        OrgMember member = orgMemberMapper.selectOne(new LambdaQueryWrapper<OrgMember>()
                .eq(OrgMember::getOrgId, orgId)
                .eq(OrgMember::getUserId, userId)
                .last("limit 1"));
        if (member == null || !STATUS_ACTIVE.equals(member.getStatus())) {
            throw new BizException(ErrorCode.ORG_FORBIDDEN, "No active organization access");
        }
        return member;
    }

    public OrgMember requireManageMember(Long userId, Long orgId) {
        OrgMember member = requireActiveMember(userId, orgId);
        if (!canManage(member.getRole())) {
            throw new BizException(ErrorCode.ORG_FORBIDDEN, "Only OWNER or ADMIN can manage organization governance");
        }
        return member;
    }

    public List<OrgMember> listActiveMemberships(Long userId) {
        return orgMemberMapper.selectList(new LambdaQueryWrapper<OrgMember>()
                .eq(OrgMember::getUserId, userId)
                .eq(OrgMember::getStatus, STATUS_ACTIVE)
                .orderByDesc(OrgMember::getUpdatedAt));
    }

    public List<OrgMember> listActiveMembers(Long orgId) {
        loadOrg(orgId);
        return orgMemberMapper.selectList(new LambdaQueryWrapper<OrgMember>()
                .eq(OrgMember::getOrgId, orgId)
                .eq(OrgMember::getStatus, STATUS_ACTIVE)
                .orderByAsc(OrgMember::getUserEmail));
    }

    public OrgMember loadActiveMemberById(Long orgId, Long memberId) {
        OrgMember member = loadMemberById(orgId, memberId);
        if (!STATUS_ACTIVE.equals(member.getStatus())) {
            throw new BizException(ErrorCode.ORG_MEMBER_NOT_FOUND, "Organization member not found");
        }
        return member;
    }

    public OrgMember loadMemberById(Long orgId, Long memberId) {
        OrgMember member = orgMemberMapper.selectOne(new LambdaQueryWrapper<OrgMember>()
                .eq(OrgMember::getOrgId, orgId)
                .eq(OrgMember::getId, memberId)
                .last("limit 1"));
        if (member == null) {
            throw new BizException(ErrorCode.ORG_MEMBER_NOT_FOUND, "Organization member not found");
        }
        return member;
    }

    public boolean canManage(String role) {
        return ROLE_OWNER.equals(role) || ROLE_ADMIN.equals(role);
    }
}
