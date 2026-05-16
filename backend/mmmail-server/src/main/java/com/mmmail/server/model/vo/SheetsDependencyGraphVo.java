package com.mmmail.server.model.vo;

import java.util.List;

public record SheetsDependencyGraphVo(
        List<SheetsFormulaGraphNodeVo> nodes,
        List<String> topologicalOrder
) {
}
