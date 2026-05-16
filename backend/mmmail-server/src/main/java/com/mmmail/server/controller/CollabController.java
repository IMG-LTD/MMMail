package com.mmmail.server.controller;

import com.mmmail.common.model.Result;
import com.mmmail.server.model.dto.CollabSnapshotRequest;
import com.mmmail.server.model.vo.CollabAwarenessVo;
import com.mmmail.server.model.vo.CollabSnapshotVo;
import com.mmmail.server.service.CollabCrdtService;
import com.mmmail.server.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/collab/{resourceType}/{resourceId}")
public class CollabController {

    private final CollabCrdtService collabCrdtService;

    public CollabController(CollabCrdtService collabCrdtService) {
        this.collabCrdtService = collabCrdtService;
    }

    @GetMapping("/snapshot")
    public Result<CollabSnapshotVo> getSnapshot(
            @PathVariable String resourceType,
            @PathVariable String resourceId
    ) {
        return Result.success(collabCrdtService.getSnapshot(SecurityUtils.currentUserId(), resourceType, resourceId));
    }

    @PostMapping("/snapshot")
    public Result<CollabSnapshotVo> writeSnapshot(
            @PathVariable String resourceType,
            @PathVariable String resourceId,
            @Valid @RequestBody CollabSnapshotRequest request
    ) {
        return Result.success(collabCrdtService.writeSnapshot(
                SecurityUtils.currentUserId(),
                resourceType,
                resourceId,
                request
        ));
    }

    @GetMapping("/awareness")
    public Result<CollabAwarenessVo> getAwareness(
            @PathVariable String resourceType,
            @PathVariable String resourceId
    ) {
        return Result.success(collabCrdtService.getAwareness(SecurityUtils.currentUserId(), resourceType, resourceId));
    }
}
