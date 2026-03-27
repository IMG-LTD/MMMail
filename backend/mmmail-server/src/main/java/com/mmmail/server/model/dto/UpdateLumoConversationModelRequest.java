package com.mmmail.server.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateLumoConversationModelRequest(
        @NotBlank @Size(min = 3, max = 32) String modelCode
) {
}
