package com.mmmail.server.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdatePassAliasRequest(
        @NotBlank @Size(max = 128) String title,
        @Size(max = 2000) String note,
        @Email @Size(max = 254) String forwardToEmail,
        List<@Email @Size(max = 254) String> forwardToEmails
) {
}
