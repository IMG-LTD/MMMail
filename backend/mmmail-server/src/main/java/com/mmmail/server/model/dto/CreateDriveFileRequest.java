package com.mmmail.server.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateDriveFileRequest(
        @NotBlank @Size(max = 128) String name,
        Long parentId,
        @Size(max = 128) String mimeType,
        @Min(1) Long sizeBytes,
        @Size(max = 512) String storagePath,
        @Size(max = 128) String checksum
) {
}
