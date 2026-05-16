package com.mmmail.server.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCommunityCommentRequest(
        @NotBlank @Size(max = 5000) String bodyMd,
        String parentCommentId
) {
}
