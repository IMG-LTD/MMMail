package com.mmmail.server.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePassMailboxRequest(
        @NotBlank @Email @Size(max = 254) String mailboxEmail
) {
}
