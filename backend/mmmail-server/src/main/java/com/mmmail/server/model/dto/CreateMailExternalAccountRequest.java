package com.mmmail.server.model.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateMailExternalAccountRequest(
        @NotBlank @Size(max = 32) String provider,
        @NotBlank @Size(max = 32) String authMode,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(max = 254) String username,
        @Size(max = 2048) String password,
        @Size(max = 4096) String oauthRefreshToken,
        @Valid @NotNull MailExternalServerRequest imap,
        @Valid @NotNull MailExternalServerRequest smtp
) {
}
