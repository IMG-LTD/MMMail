package com.mmmail.server.model.vo;

import java.util.List;

public record SearchResultVo(
        long total,
        List<SearchItemVo> items,
        SearchFacetsVo facets,
        int page,
        int size
) {
}
