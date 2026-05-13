package com.mmmail.server.model.vo;

import java.util.List;

public record V21WorkspaceSummaryVo(
        List<V21WorkspaceSummaryProductVo> productCards,
        int recommendationCount,
        String systemStatus
) {
    public V21WorkspaceSummaryVo {
        productCards = List.copyOf(productCards);
    }
}
