package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record V21WorkspaceTaskVo(
        String id,
        String title,
        boolean completed,
        LocalDateTime dueAt,
        String product
) {
}
