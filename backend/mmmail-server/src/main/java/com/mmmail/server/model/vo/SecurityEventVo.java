package com.mmmail.server.model.vo;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SecurityEventVo(
        String id,
        String type,
        String severity,
        String risk,
        List<String> reasons,
        String email,
        String ipAddress,
        String city,
        String country,
        String source,
        String detail,
        LocalDateTime lockedUntil,
        LocalDateTime acknowledgedAt,
        String actionStatus,
        String actionTaken,
        LocalDateTime actionAt,
        LocalDateTime createdAt
) {
}
