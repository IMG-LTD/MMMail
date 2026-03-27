package com.mmmail.server.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ApproveSuiteGovernanceChangeRequestRequest(
        @NotBlank String requestId,
        @Size(max = 300) String approvalNote
) {
}
