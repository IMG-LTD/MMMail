package com.mmmail.server.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ModerateCommunityReportRequest(
        @NotBlank String action,
        @Size(max = 1000) String actionNote
) {
}
