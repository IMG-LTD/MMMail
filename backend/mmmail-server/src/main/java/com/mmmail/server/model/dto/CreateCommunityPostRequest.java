package com.mmmail.server.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateCommunityPostRequest(
        String topicId,
        @NotBlank @Size(max = 180) String title,
        @NotBlank @Size(max = 20000) String bodyMd,
        List<String> tags
) {
}
