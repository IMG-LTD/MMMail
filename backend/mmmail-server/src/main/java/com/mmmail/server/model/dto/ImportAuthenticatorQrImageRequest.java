package com.mmmail.server.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ImportAuthenticatorQrImageRequest(
        @NotBlank @Size(max = 2_000_000) String dataUrl
) {
}
