package com.mmmail.server.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UndoSuiteNotificationWorkflowRequest(
        @NotBlank
        @Size(max = 64)
        String operationId
) {
}
