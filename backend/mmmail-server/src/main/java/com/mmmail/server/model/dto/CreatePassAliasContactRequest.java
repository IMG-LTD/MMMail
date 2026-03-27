package com.mmmail.server.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePassAliasContactRequest(
        @Email @NotBlank @Size(max = 254) String targetEmail,
        @Size(max = 128) String displayName,
        @Size(max = 2000) String note
) {
}
