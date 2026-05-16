package com.mmmail.server.model.vo;

public record V21CommandQuickSearchItemVo(
        String sourceType,
        String id,
        String title,
        String summary,
        String routePath,
        String productCode
) {
}
