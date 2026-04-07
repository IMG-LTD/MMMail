package com.mmmail.server.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record MailExternalAccessRequest(
        @NotBlank String mode,
        @Size(max = 255) String passwordHint,
        LocalDateTime expiresAt
) {
}
