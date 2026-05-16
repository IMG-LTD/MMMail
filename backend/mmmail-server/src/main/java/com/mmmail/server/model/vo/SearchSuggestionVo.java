package com.mmmail.server.model.vo;

public record SearchSuggestionVo(
        String moduleType,
        String resourceId,
        String title,
        String path
) {
}
