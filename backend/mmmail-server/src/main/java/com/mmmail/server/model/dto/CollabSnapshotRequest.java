package com.mmmail.server.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CollabSnapshotRequest(
        @Min(1) Integer version,
        @NotBlank String snapshotBase64
) {
}
