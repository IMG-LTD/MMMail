package com.mmmail.server.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record V21CollaborationTaskMoveRequest(
        @NotBlank @Size(max = 32) String columnId,
        @Size(max = 32) String afterTaskId,
        @Size(max = 32) String beforeTaskId
) {
}
