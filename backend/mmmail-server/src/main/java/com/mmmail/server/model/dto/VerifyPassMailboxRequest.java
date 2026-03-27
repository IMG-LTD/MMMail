package com.mmmail.server.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VerifyPassMailboxRequest(
        @NotBlank @Size(max = 32) String verificationCode
) {
}
