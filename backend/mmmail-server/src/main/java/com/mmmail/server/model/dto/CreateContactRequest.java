package com.mmmail.server.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateContactRequest(
        @NotBlank @Size(max = 64) String displayName,
        @NotBlank @Email @Size(max = 254) String email,
        @Size(max = 256) String note
) {
}
