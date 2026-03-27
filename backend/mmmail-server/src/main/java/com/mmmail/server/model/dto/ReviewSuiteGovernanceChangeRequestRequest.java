package com.mmmail.server.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ReviewSuiteGovernanceChangeRequestRequest(
        @NotBlank String requestId,
        @NotBlank @Pattern(regexp = "APPROVE|REJECT") String decision,
        @Size(max = 300) String reviewNote
) {
}
