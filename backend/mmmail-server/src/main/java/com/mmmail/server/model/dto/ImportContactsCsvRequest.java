package com.mmmail.server.model.dto;

import jakarta.validation.constraints.NotBlank;

public record ImportContactsCsvRequest(
        @NotBlank String content,
        Boolean mergeDuplicates
) {
}
