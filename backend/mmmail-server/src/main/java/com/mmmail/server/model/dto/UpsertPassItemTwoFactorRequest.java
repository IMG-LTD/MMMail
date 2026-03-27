package com.mmmail.server.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpsertPassItemTwoFactorRequest(
        @NotBlank @Size(max = 128) String issuer,
        @NotBlank @Size(max = 254) String accountName,
        @NotBlank @Size(max = 512) String secretCiphertext,
        @Size(max = 16) String algorithm,
        Integer digits,
        Integer periodSeconds
) {
}
