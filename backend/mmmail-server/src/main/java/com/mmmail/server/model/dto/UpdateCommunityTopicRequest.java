package com.mmmail.server.model.dto;

import jakarta.validation.constraints.Size;

public record UpdateCommunityTopicRequest(
        @Size(max = 64) String slug,
        @Size(max = 120) String title,
        @Size(max = 500) String description
) {
}
