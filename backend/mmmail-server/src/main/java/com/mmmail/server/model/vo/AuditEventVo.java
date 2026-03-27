package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record AuditEventVo(Long id, String eventType, String ipAddress, String detail, LocalDateTime createdAt) {
}
