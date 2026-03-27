package com.mmmail.server.model.dto;

import jakarta.validation.constraints.NotBlank;

public record ExecuteSuiteRemediationActionRequest(
        @NotBlank String actionCode
) {
}
