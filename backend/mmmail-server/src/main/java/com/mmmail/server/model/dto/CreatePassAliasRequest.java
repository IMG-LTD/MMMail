package com.mmmail.server.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreatePassAliasRequest(
        @NotBlank @Size(max = 128) String title,
        @Size(max = 2000) String note,
        @Email @Size(max = 254) String forwardToEmail,
        @Size(max = 64) String prefix,
        List<@Email @Size(max = 254) String> forwardToEmails
) {
}
