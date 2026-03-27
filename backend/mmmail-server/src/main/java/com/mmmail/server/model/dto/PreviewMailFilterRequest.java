package com.mmmail.server.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PreviewMailFilterRequest(
        @NotBlank @Email @Size(max = 254) String senderEmail,
        @Size(max = 255) String subject,
        @Size(max = 4000) String body
) {
}
