package com.mmmail.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmmail.server.commercial.FeatureCode;
import com.mmmail.server.controller.OrgAuditExportController;
import com.mmmail.server.model.vo.OrgAuditEventVo;
import com.mmmail.server.security.CommercialAuthorizationGate;
import com.mmmail.server.service.AuditService;
import com.mmmail.server.service.OrgAccessService;
import com.mmmail.server.service.OrgAuditQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BackendV22AuditExportContractTest {

    private static final long USER_ID = 42L;
    private static final long ORG_ID = 99L;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void jsonlExportWritesOneSiemReadyJsonObjectPerLine() throws Exception {
        OrgAccessService accessService = mock(OrgAccessService.class);
        AuditService auditService = mock(AuditService.class);
        OrgAuditQueryService service = new OrgAuditQueryService(accessService, auditService, OBJECT_MAPPER);
        when(auditService.listByOrgForExport(
                eq(ORG_ID), eq(20), eq(Set.of("ORG_DOMAIN_ADD")), eq(12L), eq(null), eq(null), eq(true)
        )).thenReturn(List.of(event("13", "ORG_DOMAIN_ADD")));

        OrgAuditQueryService.JsonlExportFile file = service.exportJsonlEvents(
                USER_ID, ORG_ID, 20, "ORG_DOMAIN_ADD", "12", null, null, "ASC", "203.0.113.10"
        );
        JsonNode line = readOnlyJsonLine(file);

        assertThat(file.fileName()).contains("organization-audit-" + ORG_ID).endsWith(".jsonl");
        assertThat(line.path("schemaVersion").asText()).isEqualTo("mmmail.audit.v1");
        assertThat(line.path("source").asText()).isEqualTo("mmmail");
        assertThat(line.path("id").asText()).isEqualTo("13");
        assertThat(line.path("orgId").asText()).isEqualTo(String.valueOf(ORG_ID));
        assertThat(line.path("eventType").asText()).isEqualTo("ORG_DOMAIN_ADD");
        assertThat(line.path("actorEmail").asText()).isEqualTo("owner@mmmail.local");
        assertThat(line.path("cursor").asText()).isEqualTo("13");
    }

    @Test
    void v2ControllerRequiresBusinessAuditExportFeatureBeforeReturningJsonl() {
        CommercialAuthorizationGate gate = mock(CommercialAuthorizationGate.class);
        OrgAuditQueryService service = mock(OrgAuditQueryService.class);
        OrgAuditExportController controller = new OrgAuditExportController(gate, service);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v2/orgs/99/audit/events/export");
        byte[] content = "{}\n".getBytes(StandardCharsets.UTF_8);
        when(service.exportJsonlEvents(USER_ID, ORG_ID, 20, "ORG_DOMAIN_ADD", "0", null, null, "ASC", "203.0.113.10"))
                .thenReturn(new OrgAuditQueryService.JsonlExportFile("organization-audit-99-test.jsonl", content));
        request.setRemoteAddr("203.0.113.10");

        ResponseEntity<ByteArrayResource> response = controller.exportOrgAuditEvents(
                USER_ID, ORG_ID, 20, "ORG_DOMAIN_ADD", "0", null, null, "ASC", request
        );

        verify(gate).enforceFeature(request, ORG_ID, FeatureCode.AUDIT_EXPORT);
        assertThat(response.getHeaders().getContentType().toString()).contains("application/x-ndjson");
        assertThat(response.getHeaders().getContentDisposition().getFilename()).endsWith(".jsonl");
    }

    private JsonNode readOnlyJsonLine(OrgAuditQueryService.JsonlExportFile file) throws Exception {
        List<String> lines = file.contentAsString().lines().filter(line -> !line.isBlank()).toList();
        assertThat(lines).hasSize(1);
        return OBJECT_MAPPER.readTree(lines.getFirst());
    }

    private OrgAuditEventVo event(String id, String eventType) {
        return new OrgAuditEventVo(
                id,
                String.valueOf(ORG_ID),
                String.valueOf(USER_ID),
                "owner@mmmail.local",
                eventType,
                "organization",
                String.valueOf(ORG_ID),
                "medium",
                "203.0.113.10",
                "domain=audit.example.com",
                LocalDateTime.parse("2026-05-16T08:30:00")
        );
    }
}
