package com.mmmail.server.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RollbackSuiteGovernanceChangeRequestRequest(
        @NotBlank String requestId,
        @NotBlank @Size(max = 300) String rollbackReason
) {
}
