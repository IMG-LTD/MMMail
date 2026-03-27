package com.mmmail.server.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSearchPresetRequest(
        @NotBlank @Size(max = 64) String name,
        @Size(max = 512) String keyword,
        @Size(max = 16) String folder,
        Boolean unread,
        Boolean starred,
        String from,
        String to,
        @Size(max = 32) String label
) {
}
