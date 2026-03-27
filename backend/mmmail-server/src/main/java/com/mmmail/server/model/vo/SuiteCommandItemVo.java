package com.mmmail.server.model.vo;

public record SuiteCommandItemVo(
        String commandType,
        String label,
        String description,
        String routePath,
        String actionCode,
        String productCode,
        String priority
) {
}
