package com.mmmail.server.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ExecuteSuiteBillingSubscriptionActionRequest(
        @NotBlank @Pattern(regexp = "APPLY_LATEST_DRAFT|CANCEL_AUTO_RENEW|RESUME_AUTO_RENEW") String actionCode
) {
}
