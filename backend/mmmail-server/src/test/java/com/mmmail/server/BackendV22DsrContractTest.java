package com.mmmail.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmmail.common.model.Result;
import com.mmmail.platform.jobs.JobRunRecord;
import com.mmmail.platform.jobs.JobRunRepository;
import com.mmmail.platform.jobs.JobRunRequest;
import com.mmmail.platform.jobs.JobRunState;
import com.mmmail.platform.jobs.JobRunType;
import com.mmmail.server.commercial.FeatureCode;
import com.mmmail.server.compliance.DataInventoryCatalog;
import com.mmmail.server.compliance.DataInventoryEntry;
import com.mmmail.server.compliance.DsrDeleteStrategy;
import com.mmmail.server.compliance.DsrErasureMode;
import com.mmmail.server.compliance.DsrExecutionService;
import com.mmmail.server.compliance.DsrExportStrategy;
import com.mmmail.server.compliance.DsrJobHandlerConfig;
import com.mmmail.server.compliance.DsrJobPayload;
import com.mmmail.server.compliance.DsrRequestService;
import com.mmmail.server.compliance.DsrSubjectRef;
import com.mmmail.server.controller.DsrRequestController;
import com.mmmail.server.jobs.ExplicitJobRunHandlerRegistry;
import com.mmmail.server.model.dto.DsrRequestCreateRequest;
import com.mmmail.server.model.vo.DsrJobVo;
import com.mmmail.server.security.CommercialAuthorizationGate;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BackendV22DsrContractTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Long ACTOR_USER_ID = 7L;
    private static final Long ORG_ID = 42L;
    private static final Long SUBJECT_USER_ID = 77L;

    @Test
    void serviceQueuesExportAndErasureJobsWithBusinessPayload() throws Exception {
        JobRunRepository repository = mock(JobRunRepository.class);
        when(repository.enqueue(any())).thenAnswer(invocation -> queued(invocation.getArgument(0)));
        DsrRequestService service = new DsrRequestService(repository, OBJECT_MAPPER);
        DsrRequestCreateRequest request = dsrRequest(DsrErasureMode.ANONYMIZE);

        DsrJobVo export = service.queueExport(ACTOR_USER_ID, ORG_ID, request, "req-export", "trace-export");
        DsrJobVo erasure = service.queueErasure(ACTOR_USER_ID, ORG_ID, request, "req-erase", "trace-erase");

        ArgumentCaptor<JobRunRequest> captor = ArgumentCaptor.forClass(JobRunRequest.class);
        verify(repository, times(2)).enqueue(captor.capture());
        assertThat(export.status()).isEqualTo("queued");
        assertThat(erasure.status()).isEqualTo("queued");
        assertThat(captor.getAllValues()).extracting(JobRunRequest::type)
                .containsExactly(JobRunType.DSR_EXPORT, JobRunType.DSR_ERASURE);
        assertPayload(captor.getAllValues().get(0).payloadJson(), "export");
        assertPayload(captor.getAllValues().get(1).payloadJson(), "erasure");
    }

    @Test
    void controllerRequiresBusinessDsrFeatureBeforeQueuingRequests() {
        CommercialAuthorizationGate gate = mock(CommercialAuthorizationGate.class);
        DsrRequestService service = mock(DsrRequestService.class);
        DsrRequestController controller = new DsrRequestController(gate, service);
        MockHttpServletRequest httpRequest = new MockHttpServletRequest("POST", "/api/v2/orgs/42/dsr/export");
        DsrRequestCreateRequest body = dsrRequest(DsrErasureMode.ANONYMIZE);
        DsrJobVo queued = new DsrJobVo(100L, "export", "queued", SUBJECT_USER_ID, "subject@example.com", null);
        when(service.queueExport(ACTOR_USER_ID, ORG_ID, body, "req-1", "trace-1")).thenReturn(queued);

        Result<DsrJobVo> result = controller.queueExport(
                ACTOR_USER_ID, ORG_ID, body, "req-1", "trace-1", httpRequest
        );

        verify(gate).enforceFeature(httpRequest, ORG_ID, FeatureCode.DSR_REQUESTS);
        assertThat(result.data()).isEqualTo(queued);
    }

    @Test
    void handlersPerformInventoryDrivenExportAndErasureWork() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        DataInventoryCatalog catalog = () -> List.of(
                new DataInventoryEntry("mail_message", "owner_id", DsrSubjectRef.USER_ID,
                        DsrExportStrategy.INCLUDE_ROWS, DsrDeleteStrategy.SOFT_DELETE, List.of()),
                new DataInventoryEntry("audit_event", "actor_id", DsrSubjectRef.USER_ID,
                        DsrExportStrategy.INCLUDE_ROWS, DsrDeleteStrategy.ANONYMIZE, List.of("actor_email"))
        );
        DsrExecutionService service = new DsrExecutionService(jdbcTemplate, catalog, OBJECT_MAPPER);
        when(jdbcTemplate.queryForList("select * from mail_message where owner_id = ? limit ?", SUBJECT_USER_ID, 1_000))
                .thenReturn(List.of(Map.of("id", 9L, "subject", "hello")));
        when(jdbcTemplate.queryForList("select * from audit_event where actor_id = ? limit ?", SUBJECT_USER_ID, 1_000))
                .thenReturn(List.of(Map.of("id", 10L, "actor_email", "subject@example.com")));

        String exportResult = service.exportSubject(payload(DsrErasureMode.ANONYMIZE));
        String erasureResult = service.eraseSubject(payload(DsrErasureMode.ANONYMIZE));

        assertThat(OBJECT_MAPPER.readTree(exportResult).path("tables").path("mail_message")).hasSize(1);
        assertThat(OBJECT_MAPPER.readTree(erasureResult).path("tables").path("audit_event").path("strategy").asText())
                .isEqualTo("ANONYMIZE");
        verify(jdbcTemplate).update("update mail_message set deleted = 1, updated_at = current_timestamp where owner_id = ?", SUBJECT_USER_ID);
        verify(jdbcTemplate).update(
                eq("update audit_event set actor_email = ? where actor_id = ?"),
                eq("deleted-user-77@dsr.local"),
                eq(SUBJECT_USER_ID)
        );
    }

    @Test
    void jobRunnerRegistryDiscoversTypedDsrHandlers() {
        DsrExecutionService executionService = new DsrExecutionService(mock(JdbcTemplate.class), List::of, OBJECT_MAPPER);
        DsrJobHandlerConfig config = new DsrJobHandlerConfig();
        ExplicitJobRunHandlerRegistry registry = ExplicitJobRunHandlerRegistry.fromTypedHandlers(List.of(
                config.dsrExportJobHandler(executionService),
                config.dsrErasureJobHandler(executionService)
        ));

        assertThat(registry.find(JobRunType.DSR_EXPORT)).isPresent();
        assertThat(registry.find(JobRunType.DSR_ERASURE)).isPresent();
    }

    @Test
    void dsrJobStatusUsesPublicQueuedRunningCompletedFailedVocabulary() {
        DsrJobPayload payload = payload(DsrErasureMode.ANONYMIZE);
        JobRunRecord completed = job(JobRunState.QUEUED, payload).markRunning(LocalDateTime.now())
                .markSucceeded("{\"ok\":true}", LocalDateTime.now());

        assertThat(DsrJobVo.from(completed, OBJECT_MAPPER).status()).isEqualTo("completed");
        assertThat(DsrJobVo.from(job(JobRunState.QUEUED, payload), OBJECT_MAPPER).status()).isEqualTo("queued");
    }

    private static DsrRequestCreateRequest dsrRequest(DsrErasureMode mode) {
        return new DsrRequestCreateRequest(SUBJECT_USER_ID, "subject@example.com", mode, "customer request");
    }

    private static JobRunRecord queued(JobRunRequest request) {
        return JobRunRecord.queued(100L, request, LocalDateTime.now());
    }

    private static void assertPayload(String payloadJson, String action) throws Exception {
        JsonNode payload = OBJECT_MAPPER.readTree(payloadJson);
        assertThat(payload.path("action").asText()).isEqualTo(action);
        assertThat(payload.path("orgId").asLong()).isEqualTo(ORG_ID);
        assertThat(payload.path("subjectUserId").asLong()).isEqualTo(SUBJECT_USER_ID);
        assertThat(payload.path("subjectEmail").asText()).isEqualTo("subject@example.com");
    }

    private static DsrJobPayload payload(DsrErasureMode mode) {
        return new DsrJobPayload(ORG_ID, SUBJECT_USER_ID, "subject@example.com", mode, "test", "2026-05-17T00:00:00");
    }

    private static JobRunRecord job(JobRunState state, DsrJobPayload payload) {
        JobRunRequest request = new JobRunRequest(
                JobRunType.DSR_EXPORT,
                new com.mmmail.platform.jobs.JobRunMetadata("42", "7", "req", "trace", "privacy-compliance", "export", LocalDateTime.now()),
                "dsr-subject",
                "77",
                toJson(payload, "export"),
                "dsr-req",
                3
        );
        JobRunRecord queued = JobRunRecord.queued(101L, request, LocalDateTime.now());
        if (state == JobRunState.QUEUED) {
            return queued;
        }
        return queued.markRunning(LocalDateTime.now());
    }

    private static String toJson(DsrJobPayload payload, String action) {
        try {
            return OBJECT_MAPPER.writeValueAsString(Map.of(
                    "action", action,
                    "orgId", payload.orgId(),
                    "subjectUserId", payload.subjectUserId(),
                    "subjectEmail", payload.subjectEmail(),
                    "mode", payload.mode().name(),
                    "reason", payload.reason(),
                    "requestedAt", payload.requestedAt()
            ));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
