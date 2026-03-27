package com.mmmail.server.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSuiteGovernanceChangeRequestRequest(
        @NotBlank String templateCode,
        @NotBlank @Size(max = 300) String reason,
        Long orgId,
        Long secondReviewerUserId
) {
}
