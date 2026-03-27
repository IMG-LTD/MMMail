package com.mmmail.server.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public record BatchCreateDriveShareRequest(
        @NotEmpty List<String> itemIds,
        @NotBlank @Pattern(regexp = "VIEW|EDIT") String permission,
        LocalDateTime expiresAt,
        @Size(max = 128) String password
) {
}
