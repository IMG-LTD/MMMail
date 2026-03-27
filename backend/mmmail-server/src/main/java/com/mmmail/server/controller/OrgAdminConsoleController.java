package com.mmmail.server.controller;

import com.mmmail.common.model.Result;
import com.mmmail.server.model.dto.CreateOrgCustomDomainRequest;
import com.mmmail.server.model.dto.CreateOrgMailIdentityRequest;
import com.mmmail.server.model.dto.SendOrgAuthenticationSecurityReminderRequest;
import com.mmmail.server.model.dto.UpdateOrgMemberProductAccessRequest;
import com.mmmail.server.model.vo.OrgAdminConsoleSummaryVo;
import com.mmmail.server.model.vo.OrgAuthenticationSecurityReminderResultVo;
import com.mmmail.server.model.vo.OrgAuthenticationSecurityVo;
import com.mmmail.server.model.vo.OrgCustomDomainVo;
import com.mmmail.server.model.vo.OrgMailIdentityVo;
import com.mmmail.server.model.vo.OrgMemberProductAccessVo;
import com.mmmail.server.model.vo.OrgMemberSessionVo;
import com.mmmail.server.model.vo.OrgMonitorStatusVo;
import com.mmmail.server.service.OrgAdminConsoleService;
import com.mmmail.server.service.OrgAuthenticationSecurityService;
import com.mmmail.server.service.OrgCustomDomainService;
import com.mmmail.server.service.OrgMemberSessionService;
import com.mmmail.server.service.OrgMailIdentityService;
import com.mmmail.server.service.OrgMonitorStatusService;
import com.mmmail.server.service.OrgProductAccessService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orgs")
public class OrgAdminConsoleController {

    private final OrgAdminConsoleService orgAdminConsoleService;
    private final OrgCustomDomainService orgCustomDomainService;
    private final OrgProductAccessService orgProductAccessService;
    private final OrgMailIdentityService orgMailIdentityService;
    private final OrgMemberSessionService orgMemberSessionService;
    private final OrgMonitorStatusService orgMonitorStatusService;
    private final OrgAuthenticationSecurityService orgAuthenticationSecurityService;

    public OrgAdminConsoleController(
            OrgAdminConsoleService orgAdminConsoleService,
            OrgCustomDomainService orgCustomDomainService,
            OrgProductAccessService orgProductAccessService,
            OrgMailIdentityService orgMailIdentityService,
            OrgMemberSessionService orgMemberSessionService,
            OrgMonitorStatusService orgMonitorStatusService,
            OrgAuthenticationSecurityService orgAuthenticationSecurityService
    ) {
        this.orgAdminConsoleService = orgAdminConsoleService;
        this.orgCustomDomainService = orgCustomDomainService;
        this.orgProductAccessService = orgProductAccessService;
        this.orgMailIdentityService = orgMailIdentityService;
        this.orgMemberSessionService = orgMemberSessionService;
        this.orgMonitorStatusService = orgMonitorStatusService;
        this.orgAuthenticationSecurityService = orgAuthenticationSecurityService;
    }

    @GetMapping("/{orgId}/admin-console/summary")
    public Result<OrgAdminConsoleSummaryVo> getSummary(@PathVariable Long orgId, HttpServletRequest httpRequest) {
        return Result.success(orgAdminConsoleService.getSummary(SecurityUtils.currentUserId(), orgId, httpRequest.getRemoteAddr()));
    }

    @GetMapping("/{orgId}/domains")
    public Result<List<OrgCustomDomainVo>> listDomains(@PathVariable Long orgId, HttpServletRequest httpRequest) {
        return Result.success(orgCustomDomainService.listDomains(SecurityUtils.currentUserId(), orgId, httpRequest.getRemoteAddr()));
    }

    @PostMapping("/{orgId}/domains")
    public Result<OrgCustomDomainVo> createDomain(
            @PathVariable Long orgId,
            @Valid @RequestBody CreateOrgCustomDomainRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(orgCustomDomainService.createDomain(SecurityUtils.currentUserId(), orgId, request, httpRequest.getRemoteAddr()));
    }

    @PostMapping("/{orgId}/domains/{domainId}/verify")
    public Result<OrgCustomDomainVo> verifyDomain(
            @PathVariable Long orgId,
            @PathVariable Long domainId,
            HttpServletRequest httpRequest
    ) {
        return Result.success(orgCustomDomainService.verifyDomain(SecurityUtils.currentUserId(), orgId, domainId, httpRequest.getRemoteAddr()));
    }

    @PostMapping("/{orgId}/domains/{domainId}/default")
    public Result<OrgCustomDomainVo> setDefaultDomain(
            @PathVariable Long orgId,
            @PathVariable Long domainId,
            HttpServletRequest httpRequest
    ) {
        return Result.success(orgCustomDomainService.setDefaultDomain(SecurityUtils.currentUserId(), orgId, domainId, httpRequest.getRemoteAddr()));
    }

    @DeleteMapping("/{orgId}/domains/{domainId}")
    public Result<Void> removeDomain(
            @PathVariable Long orgId,
            @PathVariable Long domainId,
            HttpServletRequest httpRequest
    ) {
        orgCustomDomainService.removeDomain(SecurityUtils.currentUserId(), orgId, domainId, httpRequest.getRemoteAddr());
        return Result.success(null);
    }

    @GetMapping("/{orgId}/mail-identities")
    public Result<List<OrgMailIdentityVo>> listMailIdentities(@PathVariable Long orgId, HttpServletRequest httpRequest) {
        return Result.success(orgMailIdentityService.listOrgIdentities(SecurityUtils.currentUserId(), orgId, httpRequest.getRemoteAddr()));
    }

    @PostMapping("/{orgId}/mail-identities")
    public Result<OrgMailIdentityVo> createMailIdentity(
            @PathVariable Long orgId,
            @Valid @RequestBody CreateOrgMailIdentityRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(orgMailIdentityService.createIdentity(SecurityUtils.currentUserId(), orgId, request, httpRequest.getRemoteAddr()));
    }

    @PostMapping("/{orgId}/mail-identities/{identityId}/default")
    public Result<OrgMailIdentityVo> setDefaultMailIdentity(
            @PathVariable Long orgId,
            @PathVariable Long identityId,
            HttpServletRequest httpRequest
    ) {
        return Result.success(orgMailIdentityService.setDefaultIdentity(SecurityUtils.currentUserId(), orgId, identityId, httpRequest.getRemoteAddr()));
    }

    @PostMapping("/{orgId}/mail-identities/{identityId}/enable")
    public Result<OrgMailIdentityVo> enableMailIdentity(
            @PathVariable Long orgId,
            @PathVariable Long identityId,
            HttpServletRequest httpRequest
    ) {
        return Result.success(orgMailIdentityService.enableIdentity(SecurityUtils.currentUserId(), orgId, identityId, httpRequest.getRemoteAddr()));
    }

    @PostMapping("/{orgId}/mail-identities/{identityId}/disable")
    public Result<OrgMailIdentityVo> disableMailIdentity(
            @PathVariable Long orgId,
            @PathVariable Long identityId,
            HttpServletRequest httpRequest
    ) {
        return Result.success(orgMailIdentityService.disableIdentity(SecurityUtils.currentUserId(), orgId, identityId, httpRequest.getRemoteAddr()));
    }

    @DeleteMapping("/{orgId}/mail-identities/{identityId}")
    public Result<Void> removeMailIdentity(
            @PathVariable Long orgId,
            @PathVariable Long identityId,
            HttpServletRequest httpRequest
    ) {
        orgMailIdentityService.removeIdentity(SecurityUtils.currentUserId(), orgId, identityId, httpRequest.getRemoteAddr());
        return Result.success(null);
    }

    @GetMapping("/{orgId}/admin-console/product-access")
    public Result<List<OrgMemberProductAccessVo>> listProductAccess(@PathVariable Long orgId, HttpServletRequest httpRequest) {
        return Result.success(orgProductAccessService.listProductAccess(SecurityUtils.currentUserId(), orgId, httpRequest.getRemoteAddr()));
    }

    @PutMapping("/{orgId}/admin-console/product-access/{memberId}")
    public Result<OrgMemberProductAccessVo> updateProductAccess(
            @PathVariable Long orgId,
            @PathVariable Long memberId,
            @Valid @RequestBody UpdateOrgMemberProductAccessRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(orgProductAccessService.updateMemberProductAccess(
                SecurityUtils.currentUserId(), orgId, memberId, request, httpRequest.getRemoteAddr()
        ));
    }

    @GetMapping("/{orgId}/admin-console/member-sessions")
    public Result<List<OrgMemberSessionVo>> listMemberSessions(
            @PathVariable Long orgId,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String memberEmail,
            @org.springframework.web.bind.annotation.RequestParam(required = false) Integer limit,
            HttpServletRequest httpRequest
    ) {
        return Result.success(orgMemberSessionService.listSessions(
                SecurityUtils.currentUserId(),
                SecurityUtils.currentPrincipal().sessionId(),
                orgId,
                memberEmail,
                limit,
                httpRequest.getRemoteAddr()
        ));
    }

    @GetMapping("/{orgId}/admin-console/monitor-status")
    public Result<OrgMonitorStatusVo> getMonitorStatus(@PathVariable Long orgId, HttpServletRequest httpRequest) {
        return Result.success(orgMonitorStatusService.getStatus(
                SecurityUtils.currentUserId(),
                SecurityUtils.currentPrincipal().sessionId(),
                orgId,
                httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/{orgId}/admin-console/member-sessions/{sessionId}/revoke")
    public Result<Void> revokeMemberSession(
            @PathVariable Long orgId,
            @PathVariable Long sessionId,
            HttpServletRequest httpRequest
    ) {
        orgMemberSessionService.revokeSession(
                SecurityUtils.currentUserId(),
                SecurityUtils.currentPrincipal().sessionId(),
                orgId,
                sessionId,
                httpRequest.getRemoteAddr()
        );
        return Result.success(null);
    }

    @GetMapping("/{orgId}/admin-console/authentication-security")
    public Result<OrgAuthenticationSecurityVo> getAuthenticationSecurity(
            @PathVariable Long orgId,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String memberEmail,
            @org.springframework.web.bind.annotation.RequestParam(required = false) Boolean onlyWithoutTwoFactor,
            @org.springframework.web.bind.annotation.RequestParam(required = false) Integer limit,
            HttpServletRequest httpRequest
    ) {
        return Result.success(orgAuthenticationSecurityService.getOverview(
                SecurityUtils.currentUserId(),
                orgId,
                memberEmail,
                onlyWithoutTwoFactor,
                limit,
                httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/{orgId}/admin-console/authentication-security/reminders")
    public Result<OrgAuthenticationSecurityReminderResultVo> sendAuthenticationSecurityReminders(
            @PathVariable Long orgId,
            @Valid @RequestBody SendOrgAuthenticationSecurityReminderRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(orgAuthenticationSecurityService.sendReminders(
                SecurityUtils.currentUserId(),
                orgId,
                request,
                httpRequest.getRemoteAddr()
        ));
    }
}
