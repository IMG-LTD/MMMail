package com.mmmail.server.controller;

import com.mmmail.common.model.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v2/workspace")
public class WorkspaceAggregationController {

    @GetMapping("/aggregation")
    public Result<Map<String, Object>> readAggregationSummary() {
        return Result.success(Map.of(
                "surfaces", new String[]{"collaboration", "command-center", "notifications"},
                "storyGroups", new String[]{"onboarding", "failure"},
                "workspaceModules", new String[]{"docs", "sheets", "mail", "calendar", "drive", "pass"}
        ));
    }
}
