package com.mmmail.server.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateBlockedDomainRequest(
        @NotBlank @Size(max = 253) String domain
) {
}
