package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record V21WorkspaceActivityItemVo(
        String id,
        String product,
        String title,
        LocalDateTime occurredAt,
        String actor
) {
}
