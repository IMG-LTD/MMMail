package com.mmmail.workspace;

import java.util.List;
import java.util.Map;

public record WorkspaceAggregationCapabilities(
        List<String> surfaces,
        List<String> storyGroups,
        List<String> workspaceModules
) {

    public static WorkspaceAggregationCapabilities defaultCapabilities() {
        return new WorkspaceAggregationCapabilities(
                List.of("collaboration", "command-center", "notifications"),
                List.of("onboarding", "failure"),
                List.of("docs", "sheets", "mail", "calendar", "drive", "pass")
        );
    }

    public Map<String, Object> toPayload() {
        return Map.of(
                "surfaces", surfaces,
                "storyGroups", storyGroups,
                "workspaceModules", workspaceModules
        );
    }
}
