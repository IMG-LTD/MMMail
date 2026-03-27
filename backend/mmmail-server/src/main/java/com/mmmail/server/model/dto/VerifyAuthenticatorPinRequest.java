package com.mmmail.server.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyAuthenticatorPinRequest(
        @NotBlank @Pattern(regexp = "\\d{4,12}") String pin
) {
}
