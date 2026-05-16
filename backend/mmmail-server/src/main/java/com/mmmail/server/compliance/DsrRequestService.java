package com.mmmail.server.compliance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.platform.jobs.JobRunMetadata;
import com.mmmail.platform.jobs.JobRunRecord;
import com.mmmail.platform.jobs.JobRunRepository;
import com.mmmail.platform.jobs.JobRunRequest;
import com.mmmail.platform.jobs.JobRunType;
import com.mmmail.server.model.dto.DsrRequestCreateRequest;
import com.mmmail.server.model.vo.DsrJobVo;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
public class DsrRequestService {

    private static final int MAX_ATTEMPTS = 3;
    private static final String AGGREGATE_TYPE = "dsr-subject";

    private final JobRunRepository repository;
    private final ObjectMapper objectMapper;

    public DsrRequestService(JobRunRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public DsrJobVo queueExport(
            Long actorUserId,
            Long orgId,
            DsrRequestCreateRequest request,
            String requestId,
            String traceId
    ) {
        return enqueue(JobRunType.DSR_EXPORT, "export", actorUserId, orgId, request, requestId, traceId);
    }

    public DsrJobVo queueErasure(
            Long actorUserId,
            Long orgId,
            DsrRequestCreateRequest request,
            String requestId,
            String traceId
    ) {
        return enqueue(JobRunType.DSR_ERASURE, "erasure", actorUserId, orgId, request, requestId, traceId);
    }

    public DsrJobVo readJob(Long orgId, Long jobId) {
        return repository.findById(jobId)
                .filter(record -> String.valueOf(orgId).equals(record.tenantId()))
                .map(record -> DsrJobVo.from(record, objectMapper))
                .orElseThrow(() -> new BizException(ErrorCode.INVALID_ARGUMENT, "DSR job not found"));
    }

    private DsrJobVo enqueue(
            JobRunType type,
            String action,
            Long actorUserId,
            Long orgId,
            DsrRequestCreateRequest request,
            String requestId,
            String traceId
    ) {
        validate(actorUserId, orgId, request);
        String effectiveRequestId = valueOrGenerated(requestId);
        JobRunRequest jobRequest = new JobRunRequest(
                type,
                metadata(type, actorUserId, orgId, effectiveRequestId, traceId),
                AGGREGATE_TYPE,
                String.valueOf(request.subjectUserId()),
                payload(action, orgId, request),
                "dsr:" + action + ":" + effectiveRequestId,
                MAX_ATTEMPTS
        );
        JobRunRecord record = repository.enqueue(jobRequest);
        return DsrJobVo.from(record, objectMapper);
    }

    private JobRunMetadata metadata(
            JobRunType type,
            Long actorUserId,
            Long orgId,
            String requestId,
            String traceId
    ) {
        return new JobRunMetadata(
                String.valueOf(orgId),
                String.valueOf(actorUserId),
                requestId,
                traceId,
                type.ownerModule(),
                type == JobRunType.DSR_EXPORT ? "export" : "erasure",
                LocalDateTime.now()
        );
    }

    private String payload(String action, Long orgId, DsrRequestCreateRequest request) {
        Map<String, Object> payload = Map.of(
                "action", action,
                "orgId", orgId,
                "subjectUserId", request.subjectUserId(),
                "subjectEmail", valueOrBlank(request.subjectEmail()),
                "mode", erasureMode(request).name(),
                "reason", valueOrBlank(request.reason()),
                "requestedAt", LocalDateTime.now().toString()
        );
        return writeJson(payload);
    }

    private void validate(Long actorUserId, Long orgId, DsrRequestCreateRequest request) {
        if (actorUserId == null || orgId == null || request == null || request.subjectUserId() == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "DSR subjectUserId, actorUserId, and orgId are required");
        }
    }

    private DsrErasureMode erasureMode(DsrRequestCreateRequest request) {
        return request.mode() == null ? DsrErasureMode.ANONYMIZE : request.mode();
    }

    private String valueOrGenerated(String requestId) {
        return requestId == null || requestId.isBlank() ? UUID.randomUUID().toString() : requestId;
    }

    private String valueOrBlank(String value) {
        return value == null ? "" : value;
    }

    private String writeJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialize DSR job payload", ex);
        }
    }
}
