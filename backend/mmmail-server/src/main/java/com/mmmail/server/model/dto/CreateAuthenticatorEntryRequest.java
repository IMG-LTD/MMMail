package com.mmmail.server.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateAuthenticatorEntryRequest(
        @NotBlank @Size(max = 128) String issuer,
        @NotBlank @Size(max = 254) String accountName,
        @NotBlank @Size(max = 512) String secretCiphertext,
        @Size(max = 16) String algorithm,
        @Min(6) @Max(8) Integer digits,
        @Min(15) @Max(120) Integer periodSeconds
) {
}
