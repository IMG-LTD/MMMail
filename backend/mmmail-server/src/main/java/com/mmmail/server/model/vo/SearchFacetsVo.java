package com.mmmail.server.model.vo;

import java.util.Map;

public record SearchFacetsVo(
        Map<String, Integer> byType
) {
}
