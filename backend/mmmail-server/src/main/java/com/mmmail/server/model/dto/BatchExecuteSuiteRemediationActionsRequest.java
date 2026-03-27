package com.mmmail.server.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BatchExecuteSuiteRemediationActionsRequest(
        @NotEmpty
        @Size(max = 20)
        List<@NotBlank String> actionCodes
) {
}
