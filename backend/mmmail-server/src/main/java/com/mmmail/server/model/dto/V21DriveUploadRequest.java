package com.mmmail.server.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record V21DriveUploadRequest(
        @NotBlank @Size(max = 128) String fileName,
        Long parentId,
        @Min(1) Long sizeBytes
) {
}
