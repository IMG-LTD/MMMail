package com.mmmail.server.compliance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DsrJobPayload(
        Long orgId,
        Long subjectUserId,
        String subjectEmail,
        DsrErasureMode mode,
        String reason,
        String requestedAt
) {
}
