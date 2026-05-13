package com.mmmail.server.model.dto;

import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record UpdateV21CollaborationTaskRequest(
        String projectId,
        @Size(max = 220) String title,
        @Size(max = 32) String status,
        @Size(max = 190) String assigneeEmail,
        LocalDateTime dueAt
) {
}
