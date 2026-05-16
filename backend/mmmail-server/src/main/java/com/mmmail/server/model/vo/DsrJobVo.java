package com.mmmail.server.model.vo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmmail.platform.jobs.JobRunRecord;
import com.mmmail.platform.jobs.JobRunState;

public record DsrJobVo(
        Long id,
        String action,
        String status,
        Long subjectUserId,
        String subjectEmail,
        String mode
) {

    public static DsrJobVo from(JobRunRecord record, ObjectMapper objectMapper) {
        JsonNode payload = readPayload(record, objectMapper);
        return new DsrJobVo(
                record.id(),
                payload.path("action").asText("unknown"),
                publicStatus(record.status()),
                payload.path("subjectUserId").asLong(),
                payload.path("subjectEmail").asText(null),
                payload.path("mode").asText(null)
        );
    }

    private static JsonNode readPayload(JobRunRecord record, ObjectMapper objectMapper) {
        try {
            return objectMapper.readTree(record.payloadJson());
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid DSR job payload", ex);
        }
    }

    private static String publicStatus(JobRunState status) {
        return switch (status) {
            case QUEUED -> "queued";
            case RUNNING, WAITING_APPROVAL, RETRYABLE -> "running";
            case SUCCEEDED -> "completed";
            case FAILED -> "failed";
        };
    }
}
