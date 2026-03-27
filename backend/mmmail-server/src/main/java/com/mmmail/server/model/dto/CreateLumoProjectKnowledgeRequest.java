package com.mmmail.server.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateLumoProjectKnowledgeRequest(
        @NotBlank @Size(min = 2, max = 128) String title,
        @NotBlank @Size(min = 1, max = 2000) String content
) {
}
