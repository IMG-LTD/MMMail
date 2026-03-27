package com.mmmail.server.service;

import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.mapper.OrgMemberMapper;
import com.mmmail.server.model.entity.OrgMember;
import com.mmmail.server.model.vo.OrgMemberVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class OrgMemberGovernanceService {

    private final OrgAccessService orgAccessService;
    private final OrgMemberMapper orgMemberMapper;
    private final AuditService auditService;

    public OrgMemberGovernanceService(
            OrgAccessService orgAccessService,
            OrgMemberMapper orgMemberMapper,
            AuditService auditService
    ) {
        this.orgAccessService = orgAccessService;
        this.orgMemberMapper = orgMemberMapper;
        this.auditService = auditService;
    }

    @Transactional
    public OrgMemberVo updateMemberStatus(
            Long userId,
            Long orgId,
            Long memberId,
            String status,
            String ipAddress
    ) {
        OrgMember actor = orgAccessService.requireManageMember(userId, orgId);
        OrgMember target = orgAccessService.loadMemberById(orgId, memberId);
        String nextStatus = normalizeStatus(status);
        validateTargetStatus(target);
        assertCanChangeStatus(actor, target, userId);
        if (nextStatus.equals(target.getStatus())) {
            return toOrgMemberVo(target);
        }

        target.setStatus(nextStatus);
        target.setUpdatedAt(LocalDateTime.now());
        orgMemberMapper.updateById(target);
        auditService.record(
                userId,
                "ORG_MEMBER_STATUS_UPDATE",
                "orgId=" + orgId + ",memberId=" + memberId + ",status=" + nextStatus,
                ipAddress,
                orgId
        );
        return toOrgMemberVo(target);
    }

    private void validateTargetStatus(OrgMember target) {
        String currentStatus = target.getStatus();
        if (OrgAccessService.STATUS_ACTIVE.equals(currentStatus)
                || OrgAccessService.STATUS_DISABLED.equals(currentStatus)) {
            return;
        }
        throw new BizException(ErrorCode.INVALID_ARGUMENT, "Only ACTIVE or DISABLED members can change status");
    }

    private void assertCanChangeStatus(OrgMember actor, OrgMember target, Long actorUserId) {
        if (OrgAccessService.ROLE_OWNER.equals(target.getRole())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Cannot disable or restore OWNER");
        }
        if (target.getUserId() != null && target.getUserId().equals(actorUserId)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Current user cannot change their own membership status");
        }
        if (OrgAccessService.ROLE_OWNER.equals(actor.getRole())) {
            return;
        }
        if (OrgAccessService.ROLE_ADMIN.equals(actor.getRole())
                && OrgAccessService.ROLE_MEMBER.equals(target.getRole())) {
            return;
        }
        throw new BizException(ErrorCode.ORG_FORBIDDEN, "No permission to change member status");
    }

    private String normalizeStatus(String status) {
        if (OrgAccessService.STATUS_ACTIVE.equals(status)
                || OrgAccessService.STATUS_DISABLED.equals(status)) {
            return status;
        }
        throw new BizException(ErrorCode.INVALID_ARGUMENT, "Status must be ACTIVE or DISABLED");
    }

    private OrgMemberVo toOrgMemberVo(OrgMember member) {
        return new OrgMemberVo(
                String.valueOf(member.getId()),
                String.valueOf(member.getOrgId()),
                member.getUserId() == null ? null : String.valueOf(member.getUserId()),
                member.getUserEmail(),
                member.getRole(),
                member.getStatus(),
                member.getInvitedBy() == null ? null : String.valueOf(member.getInvitedBy()),
                member.getJoinedAt(),
                member.getUpdatedAt()
        );
    }
}
