package com.mmmail.server.controller;

import com.mmmail.common.model.Result;
import com.mmmail.server.commercial.FeatureCode;
import com.mmmail.server.compliance.DsrRequestService;
import com.mmmail.server.model.dto.DsrRequestCreateRequest;
import com.mmmail.server.model.vo.DsrJobVo;
import com.mmmail.server.security.CommercialAuthorizationGate;
import com.mmmail.server.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/orgs")
public class DsrRequestController {

    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String TRACE_ID_HEADER = "X-B3-TraceId";

    private final CommercialAuthorizationGate commercialAuthorizationGate;
    private final DsrRequestService dsrRequestService;

    public DsrRequestController(
            CommercialAuthorizationGate commercialAuthorizationGate,
            DsrRequestService dsrRequestService
    ) {
        this.commercialAuthorizationGate = commercialAuthorizationGate;
        this.dsrRequestService = dsrRequestService;
    }

    @PostMapping("/{orgId}/dsr/export")
    public Result<DsrJobVo> queueExport(
            @PathVariable Long orgId,
            @Valid @RequestBody DsrRequestCreateRequest requestBody,
            @RequestHeader(value = REQUEST_ID_HEADER, required = false) String requestId,
            @RequestHeader(value = TRACE_ID_HEADER, required = false) String traceId,
            HttpServletRequest request
    ) {
        return queueExport(SecurityUtils.currentUserId(), orgId, requestBody, requestId, traceId, request);
    }

    public Result<DsrJobVo> queueExport(
            Long userId,
            Long orgId,
            DsrRequestCreateRequest requestBody,
            String requestId,
            String traceId,
            HttpServletRequest request
    ) {
        commercialAuthorizationGate.enforceFeature(request, orgId, FeatureCode.DSR_REQUESTS);
        return Result.success(dsrRequestService.queueExport(userId, orgId, requestBody, requestId, traceId));
    }

    @PostMapping("/{orgId}/dsr/erasure")
    public Result<DsrJobVo> queueErasure(
            @PathVariable Long orgId,
            @Valid @RequestBody DsrRequestCreateRequest requestBody,
            @RequestHeader(value = REQUEST_ID_HEADER, required = false) String requestId,
            @RequestHeader(value = TRACE_ID_HEADER, required = false) String traceId,
            HttpServletRequest request
    ) {
        commercialAuthorizationGate.enforceFeature(request, orgId, FeatureCode.DSR_REQUESTS);
        return Result.success(dsrRequestService.queueErasure(
                SecurityUtils.currentUserId(), orgId, requestBody, requestId, traceId
        ));
    }

    @GetMapping("/{orgId}/dsr/jobs/{jobId}")
    public Result<DsrJobVo> readJob(
            @PathVariable Long orgId,
            @PathVariable Long jobId,
            HttpServletRequest request
    ) {
        commercialAuthorizationGate.enforceFeature(request, orgId, FeatureCode.DSR_REQUESTS);
        return Result.success(dsrRequestService.readJob(orgId, jobId));
    }
}
