package com.mmmail.server.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateOrgTeamSpaceRequest(
        @NotBlank @Size(max = 128) String name,
        @Size(max = 256) String description,
        @Min(1) @Max(102400) Integer storageLimitMb
) {
}
