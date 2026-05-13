package com.mmmail.server.model.dto;

import jakarta.validation.constraints.Size;

public record PatchV21WorkspaceTaskRequest(
        Boolean completed,
        @Size(max = 220) String title
) {
}
