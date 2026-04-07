package com.mmmail.server.model.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public record SendMailRequest(
        Long draftId,
        @Email @NotBlank String toEmail,
        @Email String fromEmail,
        @NotBlank @Size(max = 255) String subject,
        @Size(max = 20000) String body,
        @NotBlank String idempotencyKey,
        List<String> labels,
        LocalDateTime scheduledAt,
        @Valid MailBodyE2eePayloadRequest e2ee
) {
}
