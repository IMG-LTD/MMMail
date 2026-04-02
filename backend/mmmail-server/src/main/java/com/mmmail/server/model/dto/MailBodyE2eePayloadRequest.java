package com.mmmail.server.model.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record MailBodyE2eePayloadRequest(
        @NotBlank String encryptedBody,
        @NotBlank String algorithm,
        List<String> recipientFingerprints
) {
}
