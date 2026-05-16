package com.mmmail.server.service;

public record AuditEventSpec(String eventType, String targetType, String severity) {
}
