package com.mmmail.server.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdatePassItemRequest(
        @NotBlank @Size(max = 128) String title,
        @Size(max = 16) String itemType,
        @Size(max = 255) String website,
        @Size(max = 254) String username,
        @Size(max = 512) String secretCiphertext,
        @Size(max = 2000) String note
) {
}
