package com.mmmail.server.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SaveDriveSharedWithMeRequest(
        @NotBlank @Size(min = 12, max = 80) String token,
        @Size(max = 128) String password
) {
}
