package com.mmmail.server.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCommunityTopicRequest(
        @NotBlank @Size(max = 64) String slug,
        @NotBlank @Size(max = 120) String title,
        @Size(max = 500) String description
) {
}
