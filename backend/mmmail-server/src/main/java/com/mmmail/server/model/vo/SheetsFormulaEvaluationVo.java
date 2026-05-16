package com.mmmail.server.model.vo;

import java.util.List;

public record SheetsFormulaEvaluationVo(
        List<SheetsFormulaCellResultVo> results
) {
}
