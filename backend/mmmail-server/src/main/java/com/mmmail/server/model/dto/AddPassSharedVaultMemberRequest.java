package com.mmmail.server.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddPassSharedVaultMemberRequest(
        @NotBlank @Size(max = 254) String email,
        @NotBlank @Size(max = 16) String role
) {
}
