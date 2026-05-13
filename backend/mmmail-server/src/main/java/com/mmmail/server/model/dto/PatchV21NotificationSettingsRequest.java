package com.mmmail.server.model.dto;

public record PatchV21NotificationSettingsRequest(
        Boolean emailDigest,
        Boolean productUpdates
) {
}
