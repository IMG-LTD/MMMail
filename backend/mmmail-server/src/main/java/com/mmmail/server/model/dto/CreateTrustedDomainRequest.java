package com.mmmail.server.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTrustedDomainRequest(
        @NotBlank @Size(max = 253) String domain
) {
}
