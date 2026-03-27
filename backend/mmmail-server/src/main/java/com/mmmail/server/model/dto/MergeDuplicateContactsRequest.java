package com.mmmail.server.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record MergeDuplicateContactsRequest(
        @NotBlank String primaryContactId,
        @NotEmpty List<String> duplicateContactIds
) {
}
