package com.mmmail.server.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SendLumoMessageRequest(
        @NotBlank @Size(min = 1, max = 4000) String content,
        @Size(max = 50) List<@Positive Long> knowledgeIds,
        Boolean webSearchEnabled,
        Boolean citationsEnabled,
        @Size(max = 16) String translateToLocale
) {
}
