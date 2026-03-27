package com.mmmail.server.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateClientErrorEventRequest(
        @NotBlank @Size(max = 300) String message,
        @Size(max = 64) String category,
        @Size(max = 32) String severity,
        @Size(max = 2048) String detail,
        @Size(max = 512) String path,
        @Size(max = 16) String method,
        @Size(max = 80) String requestId
) {
}
