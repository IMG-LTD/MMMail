package com.mmmail.server.model.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateMailExternalAccountRequest(
        @Size(max = 32) String provider,
        @Size(max = 32) String authMode,
        @Email @Size(max = 254) String email,
        @Size(max = 254) String username,
        @Size(max = 2048) String password,
        @Size(max = 4096) String oauthRefreshToken,
        @Valid MailExternalServerRequest imap,
        @Valid MailExternalServerRequest smtp
) {
}
