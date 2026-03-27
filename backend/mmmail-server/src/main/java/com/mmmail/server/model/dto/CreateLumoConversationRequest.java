package com.mmmail.server.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateLumoConversationRequest(
        @NotBlank @Size(min = 2, max = 128) String title,
        @Size(min = 3, max = 32) String modelCode,
        @Positive Long projectId
) {
}
