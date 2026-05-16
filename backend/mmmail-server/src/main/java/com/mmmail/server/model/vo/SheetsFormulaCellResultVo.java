package com.mmmail.server.model.vo;

import java.util.List;

public record SheetsFormulaCellResultVo(
        String ref,
        Object value,
        String type,
        List<String> dependsOn
) {
}
