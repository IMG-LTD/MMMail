package com.mmmail.server.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateSimpleLoginRelayPolicyRequest(
        @NotNull Long customDomainId,
        @NotNull Boolean catchAllEnabled,
        @NotBlank @Size(max = 32) String subdomainMode,
        @NotNull Long defaultMailboxId,
        @Size(max = 500) String note
) {
}
