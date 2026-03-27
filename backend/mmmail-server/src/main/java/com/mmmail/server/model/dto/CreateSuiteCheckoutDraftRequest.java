package com.mmmail.server.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateSuiteCheckoutDraftRequest(
        @NotBlank @Pattern(regexp = "[A-Z_]+") String offerCode,
        @NotBlank @Pattern(regexp = "MONTHLY|ANNUAL") String billingCycle,
        @Min(1) @Max(500) Integer seatCount,
        @Size(max = 80) String organizationName,
        @Size(max = 120) String domainName
) {
}
