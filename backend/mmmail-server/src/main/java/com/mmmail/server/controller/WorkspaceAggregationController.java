package com.mmmail.server.controller;

import com.mmmail.common.model.Result;
import com.mmmail.workspace.WorkspaceAggregationCapabilities;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v2/workspace")
public class WorkspaceAggregationController {

    @GetMapping("/aggregation")
    public Result<Map<String, Object>> readAggregationSummary() {
        return Result.success(WorkspaceAggregationCapabilities.defaultCapabilities().toPayload());
    }
}
