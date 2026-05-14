package com.mmmail.server.model.vo;

public record PublicSystemStatusVo(
        String incidentMessage,
        String maintenanceWindow,
        String status,
        String updatedAt
) {
}
