package com.mmmail.server.model.dto;

import com.mmmail.server.compliance.DsrErasureMode;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DsrRequestCreateRequest(
        @NotNull Long subjectUserId,
        @Email String subjectEmail,
        DsrErasureMode mode,
        @Size(max = 512) String reason
) {
}
