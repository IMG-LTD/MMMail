package com.mmmail.server.controller;

import com.mmmail.common.model.Result;
import com.mmmail.server.model.dto.CreateOrgCustomDomainRequest;
import com.mmmail.server.model.vo.DomainDnsDiagnosticsVo;
import com.mmmail.server.model.vo.DomainDnsRecordsVo;
import com.mmmail.server.model.vo.OrgCustomDomainVo;
import com.mmmail.server.security.RequireEntitlement;
import com.mmmail.server.service.OrgCustomDomainService;
import com.mmmail.server.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequireEntitlement("HOSTED")
@RequestMapping("/api/v1/domains")
public class DomainController {

    private static final String ORG_HEADER = "X-Org-Id";

    private final OrgCustomDomainService orgCustomDomainService;

    public DomainController(OrgCustomDomainService orgCustomDomainService) {
        this.orgCustomDomainService = orgCustomDomainService;
    }

    @GetMapping
    public Result<List<OrgCustomDomainVo>> list(@RequestHeader(ORG_HEADER) Long orgId, HttpServletRequest request) {
        return Result.success(orgCustomDomainService.listDomains(SecurityUtils.currentUserId(), orgId, request.getRemoteAddr()));
    }

    @PostMapping
    public Result<OrgCustomDomainVo> create(
            @RequestHeader(ORG_HEADER) Long orgId,
            @Valid @RequestBody CreateOrgCustomDomainRequest body,
            HttpServletRequest request
    ) {
        return Result.success(orgCustomDomainService.createDomain(SecurityUtils.currentUserId(), orgId, body, request.getRemoteAddr()));
    }

    @GetMapping("/{domainId}")
    public Result<OrgCustomDomainVo> read(@RequestHeader(ORG_HEADER) Long orgId, @PathVariable Long domainId, HttpServletRequest request) {
        return Result.success(orgCustomDomainService.getDomain(SecurityUtils.currentUserId(), orgId, domainId, request.getRemoteAddr()));
    }

    @DeleteMapping("/{domainId}")
    public Result<Void> delete(@RequestHeader(ORG_HEADER) Long orgId, @PathVariable Long domainId, HttpServletRequest request) {
        orgCustomDomainService.removeDomain(SecurityUtils.currentUserId(), orgId, domainId, request.getRemoteAddr());
        return Result.success(null);
    }

    @PostMapping("/{domainId}/verify")
    public Result<OrgCustomDomainVo> verify(@RequestHeader(ORG_HEADER) Long orgId, @PathVariable Long domainId, HttpServletRequest request) {
        return Result.success(orgCustomDomainService.verifyDomainWithDns(SecurityUtils.currentUserId(), orgId, domainId, request.getRemoteAddr()));
    }

    @GetMapping("/{domainId}/dns-records")
    public Result<DomainDnsRecordsVo> dnsRecords(@RequestHeader(ORG_HEADER) Long orgId, @PathVariable Long domainId, HttpServletRequest request) {
        return Result.success(orgCustomDomainService.getExpectedDnsRecords(SecurityUtils.currentUserId(), orgId, domainId, request.getRemoteAddr()));
    }

    @GetMapping("/{domainId}/diagnostics")
    public Result<DomainDnsDiagnosticsVo> diagnostics(@RequestHeader(ORG_HEADER) Long orgId, @PathVariable Long domainId, HttpServletRequest request) {
        return Result.success(orgCustomDomainService.diagnoseDomain(SecurityUtils.currentUserId(), orgId, domainId, request.getRemoteAddr()));
    }
}
