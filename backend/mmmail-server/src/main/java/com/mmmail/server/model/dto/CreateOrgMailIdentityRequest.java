package com.mmmail.server.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateOrgMailIdentityRequest(
        @NotNull Long memberId,
        @NotNull Long customDomainId,
        @NotBlank @Size(max = 64) String localPart,
        @Size(max = 64) String displayName
) {
}
