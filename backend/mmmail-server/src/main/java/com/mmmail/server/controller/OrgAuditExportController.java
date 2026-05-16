package com.mmmail.server.controller;

import com.mmmail.server.commercial.FeatureCode;
import com.mmmail.server.security.CommercialAuthorizationGate;
import com.mmmail.server.service.OrgAuditQueryService;
import com.mmmail.server.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v2/orgs")
public class OrgAuditExportController {

    private static final String JSONL_MEDIA_TYPE = "application/x-ndjson";

    private final CommercialAuthorizationGate commercialAuthorizationGate;
    private final OrgAuditQueryService orgAuditQueryService;

    public OrgAuditExportController(
            CommercialAuthorizationGate commercialAuthorizationGate,
            OrgAuditQueryService orgAuditQueryService
    ) {
        this.commercialAuthorizationGate = commercialAuthorizationGate;
        this.orgAuditQueryService = orgAuditQueryService;
    }

    @GetMapping("/{orgId}/audit/events/export")
    public ResponseEntity<ByteArrayResource> exportOrgAuditEvents(
            @PathVariable Long orgId,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String eventTypes,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) String sortDirection,
            HttpServletRequest request
    ) {
        return exportOrgAuditEvents(
                SecurityUtils.currentUserId(),
                orgId,
                limit,
                eventTypes,
                cursor,
                fromDate,
                toDate,
                sortDirection,
                request
        );
    }

    public ResponseEntity<ByteArrayResource> exportOrgAuditEvents(
            Long userId,
            Long orgId,
            Integer limit,
            String eventTypes,
            String cursor,
            String fromDate,
            String toDate,
            String sortDirection,
            HttpServletRequest request
    ) {
        commercialAuthorizationGate.enforceFeature(request, orgId, FeatureCode.AUDIT_EXPORT);
        OrgAuditQueryService.JsonlExportFile file = orgAuditQueryService.exportJsonlEvents(
                userId, orgId, limit, eventTypes, cursor, fromDate, toDate, sortDirection, request.getRemoteAddr()
        );
        return toJsonlResponse(file.fileName(), file.content());
    }

    private ResponseEntity<ByteArrayResource> toJsonlResponse(String fileName, byte[] content) {
        ByteArrayResource resource = new ByteArrayResource(content);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition(fileName))
                .contentType(MediaType.parseMediaType(JSONL_MEDIA_TYPE))
                .contentLength(content.length)
                .body(resource);
    }

    private String contentDisposition(String fileName) {
        return ContentDisposition.attachment().filename(fileName, StandardCharsets.UTF_8).build().toString();
    }
}
