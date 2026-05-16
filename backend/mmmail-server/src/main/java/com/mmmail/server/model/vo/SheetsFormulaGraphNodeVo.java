package com.mmmail.server.model.vo;

import java.util.List;

public record SheetsFormulaGraphNodeVo(
        String ref,
        String formula,
        List<String> dependsOn,
        List<String> dependents
) {
}
