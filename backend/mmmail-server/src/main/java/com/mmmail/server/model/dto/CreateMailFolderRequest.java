package com.mmmail.server.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateMailFolderRequest(
        @NotBlank @Size(max = 64) String name,
        @Size(max = 7) @Pattern(regexp = "^#[0-9A-Fa-f]{6}$") String color,
        Long parentId,
        Boolean notificationsEnabled
) {
}
