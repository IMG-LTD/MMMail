package com.mmmail.server.controller;

import com.mmmail.common.model.Result;
import com.mmmail.server.model.dto.CreateOrgTeamSpaceMemberRequest;
import com.mmmail.server.model.dto.UpdateOrgTeamSpaceMemberRoleRequest;
import com.mmmail.server.model.vo.OrgTeamSpaceActivityVo;
import com.mmmail.server.model.vo.OrgTeamSpaceFileVersionVo;
import com.mmmail.server.model.vo.OrgTeamSpaceItemVo;
import com.mmmail.server.model.vo.OrgTeamSpaceMemberVo;
import com.mmmail.server.model.vo.OrgTeamSpaceTrashItemVo;
import com.mmmail.server.service.OrgTeamSpaceFileService;
import com.mmmail.server.service.OrgTeamSpaceGovernanceService;
import com.mmmail.server.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orgs")
public class OrgTeamSpaceGovernanceController {

    private final OrgTeamSpaceGovernanceService orgTeamSpaceGovernanceService;
    private final OrgTeamSpaceFileService orgTeamSpaceFileService;

    public OrgTeamSpaceGovernanceController(
            OrgTeamSpaceGovernanceService orgTeamSpaceGovernanceService,
            OrgTeamSpaceFileService orgTeamSpaceFileService
    ) {
        this.orgTeamSpaceGovernanceService = orgTeamSpaceGovernanceService;
        this.orgTeamSpaceFileService = orgTeamSpaceFileService;
    }

    @GetMapping("/{orgId}/team-spaces/{teamSpaceId}/members")
    public Result<List<OrgTeamSpaceMemberVo>> listMembers(
            @PathVariable Long orgId,
            @PathVariable Long teamSpaceId,
            HttpServletRequest httpRequest
    ) {
        return Result.success(orgTeamSpaceGovernanceService.listMembers(
                SecurityUtils.currentUserId(), orgId, teamSpaceId, httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/{orgId}/team-spaces/{teamSpaceId}/members")
    public Result<OrgTeamSpaceMemberVo> addMember(
            @PathVariable Long orgId,
            @PathVariable Long teamSpaceId,
            @Valid @RequestBody CreateOrgTeamSpaceMemberRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(orgTeamSpaceGovernanceService.addMember(
                SecurityUtils.currentUserId(), orgId, teamSpaceId, request, httpRequest.getRemoteAddr()
        ));
    }

    @PutMapping("/{orgId}/team-spaces/{teamSpaceId}/members/{memberId}")
    public Result<OrgTeamSpaceMemberVo> updateMemberRole(
            @PathVariable Long orgId,
            @PathVariable Long teamSpaceId,
            @PathVariable Long memberId,
            @Valid @RequestBody UpdateOrgTeamSpaceMemberRoleRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(orgTeamSpaceGovernanceService.updateMemberRole(
                SecurityUtils.currentUserId(), orgId, teamSpaceId, memberId, request, httpRequest.getRemoteAddr()
        ));
    }

    @DeleteMapping("/{orgId}/team-spaces/{teamSpaceId}/members/{memberId}")
    public Result<Void> removeMember(
            @PathVariable Long orgId,
            @PathVariable Long teamSpaceId,
            @PathVariable Long memberId,
            HttpServletRequest httpRequest
    ) {
        orgTeamSpaceGovernanceService.removeMember(
                SecurityUtils.currentUserId(), orgId, teamSpaceId, memberId, httpRequest.getRemoteAddr()
        );
        return Result.success(null);
    }

    @GetMapping("/{orgId}/team-spaces/{teamSpaceId}/files/{itemId}/versions")
    public Result<List<OrgTeamSpaceFileVersionVo>> listFileVersions(
            @PathVariable Long orgId,
            @PathVariable Long teamSpaceId,
            @PathVariable Long itemId,
            @RequestParam(required = false) Integer limit
    ) {
        return Result.success(orgTeamSpaceFileService.listFileVersions(
                SecurityUtils.currentUserId(), orgId, teamSpaceId, itemId, limit
        ));
    }

    @PostMapping("/{orgId}/team-spaces/{teamSpaceId}/files/{itemId}/versions")
    public Result<OrgTeamSpaceItemVo> uploadFileVersion(
            @PathVariable Long orgId,
            @PathVariable Long teamSpaceId,
            @PathVariable Long itemId,
            @RequestParam("file") MultipartFile file,
            HttpServletRequest httpRequest
    ) {
        return Result.success(orgTeamSpaceFileService.uploadFileVersion(
                SecurityUtils.currentUserId(), orgId, teamSpaceId, itemId, file, httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/{orgId}/team-spaces/{teamSpaceId}/files/{itemId}/versions/{versionId}/restore")
    public Result<OrgTeamSpaceItemVo> restoreFileVersion(
            @PathVariable Long orgId,
            @PathVariable Long teamSpaceId,
            @PathVariable Long itemId,
            @PathVariable Long versionId,
            HttpServletRequest httpRequest
    ) {
        return Result.success(orgTeamSpaceFileService.restoreFileVersion(
                SecurityUtils.currentUserId(), orgId, teamSpaceId, itemId, versionId, httpRequest.getRemoteAddr()
        ));
    }

    @DeleteMapping("/{orgId}/team-spaces/{teamSpaceId}/items/{itemId}")
    public Result<Void> deleteItem(
            @PathVariable Long orgId,
            @PathVariable Long teamSpaceId,
            @PathVariable Long itemId,
            HttpServletRequest httpRequest
    ) {
        orgTeamSpaceFileService.deleteItem(
                SecurityUtils.currentUserId(), orgId, teamSpaceId, itemId, httpRequest.getRemoteAddr()
        );
        return Result.success(null);
    }

    @GetMapping("/{orgId}/team-spaces/{teamSpaceId}/trash")
    public Result<List<OrgTeamSpaceTrashItemVo>> listTrashItems(
            @PathVariable Long orgId,
            @PathVariable Long teamSpaceId,
            @RequestParam(required = false) Integer limit
    ) {
        return Result.success(orgTeamSpaceFileService.listTrashItems(
                SecurityUtils.currentUserId(), orgId, teamSpaceId, limit
        ));
    }

    @PostMapping("/{orgId}/team-spaces/{teamSpaceId}/trash/{itemId}/restore")
    public Result<OrgTeamSpaceTrashItemVo> restoreTrashItem(
            @PathVariable Long orgId,
            @PathVariable Long teamSpaceId,
            @PathVariable Long itemId,
            HttpServletRequest httpRequest
    ) {
        return Result.success(orgTeamSpaceFileService.restoreTrashItem(
                SecurityUtils.currentUserId(), orgId, teamSpaceId, itemId, httpRequest.getRemoteAddr()
        ));
    }

    @DeleteMapping("/{orgId}/team-spaces/{teamSpaceId}/trash/{itemId}")
    public Result<Void> purgeTrashItem(
            @PathVariable Long orgId,
            @PathVariable Long teamSpaceId,
            @PathVariable Long itemId,
            HttpServletRequest httpRequest
    ) {
        orgTeamSpaceFileService.purgeTrashItem(
                SecurityUtils.currentUserId(), orgId, teamSpaceId, itemId, httpRequest.getRemoteAddr()
        );
        return Result.success(null);
    }

    @GetMapping("/{orgId}/team-spaces/{teamSpaceId}/activity")
    public Result<List<OrgTeamSpaceActivityVo>> listActivities(
            @PathVariable Long orgId,
            @PathVariable Long teamSpaceId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer limit
    ) {
        return Result.success(orgTeamSpaceGovernanceService.listActivities(
                SecurityUtils.currentUserId(), orgId, teamSpaceId, category, limit
        ));
    }
}
