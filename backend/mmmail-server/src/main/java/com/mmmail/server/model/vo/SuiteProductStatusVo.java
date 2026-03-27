package com.mmmail.server.model.vo;

import java.util.List;

public record SuiteProductStatusVo(
        String code,
        String name,
        String status,
        String category,
        String description,
        boolean enabledByPlan,
        List<String> highlights
) {
}
