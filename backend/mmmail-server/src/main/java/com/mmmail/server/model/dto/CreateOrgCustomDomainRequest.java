package com.mmmail.server.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateOrgCustomDomainRequest(
        @NotBlank @Size(max = 255) String domain
) {
}
