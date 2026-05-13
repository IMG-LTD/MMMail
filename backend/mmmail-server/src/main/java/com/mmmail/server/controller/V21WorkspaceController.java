package com.mmmail.server.controller;

import com.mmmail.common.model.Result;
import com.mmmail.server.model.dto.PatchV21WorkspaceTaskRequest;
import com.mmmail.server.model.vo.V21WorkspaceActivityItemVo;
import com.mmmail.server.model.vo.V21WorkspaceSummaryVo;
import com.mmmail.server.model.vo.V21WorkspaceTaskVo;
import com.mmmail.server.service.V21WorkspaceRuntimeBridgeService;
import com.mmmail.server.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v2/workspace")
public class V21WorkspaceController {

    private final V21WorkspaceRuntimeBridgeService workspaceRuntimeBridgeService;

    public V21WorkspaceController(V21WorkspaceRuntimeBridgeService workspaceRuntimeBridgeService) {
        this.workspaceRuntimeBridgeService = workspaceRuntimeBridgeService;
    }

    @GetMapping("/summary")
    public Result<V21WorkspaceSummaryVo> summary(HttpServletRequest request) {
        return Result.success(workspaceRuntimeBridgeService.summary(SecurityUtils.currentUserId(), request));
    }

    @GetMapping("/activity")
    public Result<List<V21WorkspaceActivityItemVo>> activity(HttpServletRequest request) {
        return Result.success(workspaceRuntimeBridgeService.activity(SecurityUtils.currentUserId(), request));
    }

    @GetMapping("/tasks")
    public Result<List<V21WorkspaceTaskVo>> tasks(HttpServletRequest request) {
        return Result.success(workspaceRuntimeBridgeService.tasks(SecurityUtils.currentUserId(), request));
    }

    @PatchMapping("/tasks/{id}")
    public Result<V21WorkspaceTaskVo> patchTask(
            @PathVariable String id,
            @Valid @RequestBody PatchV21WorkspaceTaskRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(workspaceRuntimeBridgeService.patchTask(
                SecurityUtils.currentUserId(),
                id,
                request,
                httpRequest
        ));
    }
}
