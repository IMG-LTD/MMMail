package com.mmmail.server.model.dto;

import java.time.LocalDateTime;

public record SearchQueryCriteria(
        String keyword,
        String types,
        int page,
        int size,
        LocalDateTime from,
        LocalDateTime to,
        Long orgId
) {
}
