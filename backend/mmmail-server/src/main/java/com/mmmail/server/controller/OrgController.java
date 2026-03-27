package com.mmmail.server.controller;

import com.mmmail.common.model.Result;
import com.mmmail.server.model.dto.BatchRemoveOrgMembersRequest;
import com.mmmail.server.model.dto.BatchUpdateOrgMemberRoleRequest;
import com.mmmail.server.model.dto.CreateOrgRequest;
import com.mmmail.server.model.dto.InviteOrgMemberRequest;
import com.mmmail.server.model.dto.RespondOrgInviteRequest;
import com.mmmail.server.model.dto.UpdateOrgPolicyRequest;
import com.mmmail.server.model.dto.UpdateOrgMemberRoleRequest;
import com.mmmail.server.model.dto.UpdateOrgMemberStatusRequest;
import com.mmmail.server.model.vo.OrgAuditEventVo;
import com.mmmail.server.model.vo.OrgAccessScopeVo;
import com.mmmail.server.model.vo.OrgBatchActionResultVo;
import com.mmmail.server.model.vo.OrgIncomingInviteVo;
import com.mmmail.server.model.vo.OrgMemberVo;
import com.mmmail.server.model.vo.OrgPolicyVo;
import com.mmmail.server.model.vo.OrgWorkspaceVo;
import com.mmmail.server.service.OrgAuditQueryService;
import com.mmmail.server.service.OrgMemberGovernanceService;
import com.mmmail.server.service.OrgProductAccessService;
import com.mmmail.server.service.OrgService;
import com.mmmail.server.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/v1/orgs")
public class OrgController {

    private static final String CSV_MEDIA_TYPE = "text/csv";

    private final OrgService orgService;
    private final OrgMemberGovernanceService orgMemberGovernanceService;
    private final OrgAuditQueryService orgAuditQueryService;
    private final OrgProductAccessService orgProductAccessService;

    public OrgController(
            OrgService orgService,
            OrgMemberGovernanceService orgMemberGovernanceService,
            OrgAuditQueryService orgAuditQueryService,
            OrgProductAccessService orgProductAccessService
    ) {
        this.orgService = orgService;
        this.orgMemberGovernanceService = orgMemberGovernanceService;
        this.orgAuditQueryService = orgAuditQueryService;
        this.orgProductAccessService = orgProductAccessService;
    }

    @PostMapping
    public Result<OrgWorkspaceVo> createOrg(
            @Valid @RequestBody CreateOrgRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(orgService.createOrg(
                SecurityUtils.currentUserId(),
                request,
                httpRequest.getRemoteAddr()
        ));
    }

    @GetMapping
    public Result<List<OrgWorkspaceVo>> listOrgs(HttpServletRequest httpRequest) {
        return Result.success(orgService.listOrgs(SecurityUtils.currentUserId(), httpRequest.getRemoteAddr()));
    }

    @GetMapping("/access-context")
    public Result<List<OrgAccessScopeVo>> listAccessContext(HttpServletRequest httpRequest) {
        return Result.success(orgProductAccessService.listCurrentUserAccessScopes(
                SecurityUtils.currentUserId(),
                httpRequest.getRemoteAddr()
        ));
    }

    @GetMapping("/{orgId}/members")
    public Result<List<OrgMemberVo>> listMembers(
            @PathVariable Long orgId,
            HttpServletRequest httpRequest
    ) {
        return Result.success(orgService.listMembers(SecurityUtils.currentUserId(), orgId, httpRequest.getRemoteAddr()));
    }

    @GetMapping("/{orgId}/policy")
    public Result<OrgPolicyVo> getPolicy(
            @PathVariable Long orgId,
            HttpServletRequest httpRequest
    ) {
        return Result.success(orgService.getPolicy(SecurityUtils.currentUserId(), orgId, httpRequest.getRemoteAddr()));
    }

    @PutMapping("/{orgId}/policy")
    public Result<OrgPolicyVo> updatePolicy(
            @PathVariable Long orgId,
            @Valid @RequestBody UpdateOrgPolicyRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(orgService.updatePolicy(
                SecurityUtils.currentUserId(),
                orgId,
                request,
                httpRequest.getRemoteAddr()
        ));
    }

    @PutMapping("/{orgId}/members/{memberId}/role")
    public Result<OrgMemberVo> updateMemberRole(
            @PathVariable Long orgId,
            @PathVariable Long memberId,
            @Valid @RequestBody UpdateOrgMemberRoleRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(orgService.updateMemberRole(
                SecurityUtils.currentUserId(),
                orgId,
                memberId,
                request,
                httpRequest.getRemoteAddr()
        ));
    }

    @PutMapping("/{orgId}/members/{memberId}/status")
    public Result<OrgMemberVo> updateMemberStatus(
            @PathVariable Long orgId,
            @PathVariable Long memberId,
            @Valid @RequestBody UpdateOrgMemberStatusRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(orgMemberGovernanceService.updateMemberStatus(
                SecurityUtils.currentUserId(),
                orgId,
                memberId,
                request.status(),
                httpRequest.getRemoteAddr()
        ));
    }

    @DeleteMapping("/{orgId}/members/{memberId}")
    public Result<Void> removeMember(
            @PathVariable Long orgId,
            @PathVariable Long memberId,
            HttpServletRequest httpRequest
    ) {
        orgService.removeMember(SecurityUtils.currentUserId(), orgId, memberId, httpRequest.getRemoteAddr());
        return Result.success(null);
    }

    @PostMapping("/{orgId}/members/batch/role")
    public Result<OrgBatchActionResultVo> batchUpdateMemberRole(
            @PathVariable Long orgId,
            @Valid @RequestBody BatchUpdateOrgMemberRoleRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(orgService.batchUpdateMemberRole(
                SecurityUtils.currentUserId(),
                orgId,
                request,
                httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/{orgId}/members/batch/remove")
    public Result<OrgBatchActionResultVo> batchRemoveMembers(
            @PathVariable Long orgId,
            @Valid @RequestBody BatchRemoveOrgMembersRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(orgService.batchRemoveMembers(
                SecurityUtils.currentUserId(),
                orgId,
                request,
                httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/{orgId}/invites")
    public Result<OrgMemberVo> inviteMember(
            @PathVariable Long orgId,
            @Valid @RequestBody InviteOrgMemberRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(orgService.inviteMember(
                SecurityUtils.currentUserId(),
                orgId,
                request,
                httpRequest.getRemoteAddr()
        ));
    }

    @GetMapping("/invites/incoming")
    public Result<List<OrgIncomingInviteVo>> listIncomingInvites(HttpServletRequest httpRequest) {
        return Result.success(orgService.listIncomingInvites(SecurityUtils.currentUserId(), httpRequest.getRemoteAddr()));
    }

    @PostMapping("/invites/{inviteId}/respond")
    public Result<OrgIncomingInviteVo> respondInvite(
            @PathVariable Long inviteId,
            @Valid @RequestBody RespondOrgInviteRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(orgService.respondInvite(
                SecurityUtils.currentUserId(),
                inviteId,
                request,
                httpRequest.getRemoteAddr()
        ));
    }

    @GetMapping("/{orgId}/audit/events")
    public Result<List<OrgAuditEventVo>> listOrgAuditEvents(
            @PathVariable Long orgId,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String actorEmail,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) String sortDirection,
            HttpServletRequest httpRequest
    ) {
        return Result.success(orgAuditQueryService.listEvents(
                SecurityUtils.currentUserId(),
                orgId,
                limit,
                eventType,
                actorEmail,
                keyword,
                fromDate,
                toDate,
                sortDirection,
                httpRequest.getRemoteAddr()
        ));
    }

    @GetMapping("/{orgId}/audit/events/export")
    public ResponseEntity<ByteArrayResource> exportOrgAuditEvents(
            @PathVariable Long orgId,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String actorEmail,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) String sortDirection,
            HttpServletRequest httpRequest
    ) {
        OrgAuditQueryService.CsvExportFile file = orgAuditQueryService.exportEvents(
                SecurityUtils.currentUserId(),
                orgId,
                limit,
                eventType,
                actorEmail,
                keyword,
                fromDate,
                toDate,
                sortDirection,
                httpRequest.getRemoteAddr()
        );
        return toCsvResponse(file.fileName(), file.content());
    }

    private ResponseEntity<ByteArrayResource> toCsvResponse(String fileName, byte[] content) {
        ByteArrayResource resource = new ByteArrayResource(content);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(fileName, StandardCharsets.UTF_8).build().toString())
                .contentType(MediaType.parseMediaType(CSV_MEDIA_TYPE))
                .contentLength(content.length)
                .body(resource);
    }
}
