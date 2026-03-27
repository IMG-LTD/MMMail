package com.mmmail.server.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record SaveDraftRequest(
        Long draftId,
        @Email String toEmail,
        @Email String fromEmail,
        @Size(max = 255) String subject,
        @Size(max = 20000) String body
) {
}
