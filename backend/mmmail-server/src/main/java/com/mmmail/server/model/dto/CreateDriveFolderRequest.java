package com.mmmail.server.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateDriveFolderRequest(
        @NotBlank @Size(max = 128) String name,
        Long parentId
) {
}
