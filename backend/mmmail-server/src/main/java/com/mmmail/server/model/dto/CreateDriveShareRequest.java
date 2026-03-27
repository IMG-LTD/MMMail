package com.mmmail.server.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record CreateDriveShareRequest(
        @NotBlank @Pattern(regexp = "VIEW|EDIT") String permission,
        LocalDateTime expiresAt,
        @Size(max = 128) String password
) {
}
