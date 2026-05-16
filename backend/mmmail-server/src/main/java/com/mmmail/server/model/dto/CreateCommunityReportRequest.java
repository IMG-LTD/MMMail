package com.mmmail.server.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCommunityReportRequest(
        @NotBlank String targetType,
        @NotBlank String targetId,
        @NotBlank @Size(max = 64) String reason,
        @Size(max = 1000) String detail
) {
}
