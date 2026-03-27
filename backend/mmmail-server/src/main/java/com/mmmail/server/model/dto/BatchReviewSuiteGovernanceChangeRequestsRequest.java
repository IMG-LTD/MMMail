package com.mmmail.server.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BatchReviewSuiteGovernanceChangeRequestsRequest(
        @NotEmpty
        @Size(max = 30)
        List<@NotBlank String> requestIds,
        @NotBlank
        @Pattern(regexp = "APPROVE|REJECT")
        String decision,
        @Size(max = 300)
        String reviewNote
) {
}
