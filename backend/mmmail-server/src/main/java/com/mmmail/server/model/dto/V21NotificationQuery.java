package com.mmmail.server.model.dto;

public record V21NotificationQuery(
        Integer limit,
        Boolean unreadOnly,
        String status,
        Boolean includeSnoozed
) {
}
