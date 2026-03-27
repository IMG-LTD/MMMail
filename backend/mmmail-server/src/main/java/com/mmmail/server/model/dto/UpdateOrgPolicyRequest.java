package com.mmmail.server.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateOrgPolicyRequest(
        @Size(max = 20) List<@Size(min = 1, max = 64) String> allowedEmailDomains,
        @Min(1) @Max(5000) Integer memberLimit,
        @Min(1) @Max(168) Integer governanceReviewSlaHours,
        Boolean adminCanInviteAdmin,
        Boolean adminCanRemoveAdmin,
        Boolean adminCanReviewGovernance,
        Boolean adminCanExecuteGovernance,
        Boolean requireDualReviewGovernance,
        @Pattern(regexp = "OFF|ADMINS|ALL") String twoFactorEnforcementLevel,
        @Min(0) @Max(90) Integer twoFactorGracePeriodDays
) {
}
