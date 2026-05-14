package com.mmmail.server.controller;

import com.mmmail.common.model.Result;
import com.mmmail.server.model.dto.CreateDriveShareRequest;
import com.mmmail.server.model.dto.V21DriveFileUpdateRequest;
import com.mmmail.server.model.dto.V21DriveUploadRequest;
import com.mmmail.server.model.vo.DriveItemVo;
import com.mmmail.server.model.vo.DriveShareLinkVo;
import com.mmmail.server.model.vo.DriveUsageVo;
import com.mmmail.server.service.V21DriveRuntimeBridgeService;
import com.mmmail.server.util.ClientIpResolver;
import com.mmmail.server.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Validated
@RequestMapping("/api/v2/drive")
public class V21DriveController {

    private final V21DriveRuntimeBridgeService driveRuntimeBridgeService;
    private final ClientIpResolver clientIpResolver;

    public V21DriveController(
            V21DriveRuntimeBridgeService driveRuntimeBridgeService,
            ClientIpResolver clientIpResolver
    ) {
        this.driveRuntimeBridgeService = driveRuntimeBridgeService;
        this.clientIpResolver = clientIpResolver;
    }

    @GetMapping("/files")
    public Result<List<DriveItemVo>> files(
            @RequestParam(required = false) Long parentId,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(required = false) Integer limit
    ) {
        return Result.success(driveRuntimeBridgeService.listFiles(SecurityUtils.currentUserId(), parentId, keyword, limit));
    }

    @GetMapping("/folders")
    public Result<List<DriveItemVo>> folders(
            @RequestParam(required = false) Long parentId,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(required = false) Integer limit
    ) {
        return Result.success(driveRuntimeBridgeService.listFolders(SecurityUtils.currentUserId(), parentId, keyword, limit));
    }

    @PostMapping("/uploads")
    public Result<DriveItemVo> createUpload(
            @Valid @RequestBody V21DriveUploadRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(driveRuntimeBridgeService.createUpload(
                SecurityUtils.currentUserId(),
                request,
                clientIpResolver.resolve(httpRequest)
        ));
    }

    @GetMapping("/uploads/{itemId}")
    public Result<DriveItemVo> readUpload(@PathVariable Long itemId) {
        return Result.success(driveRuntimeBridgeService.readUpload(SecurityUtils.currentUserId(), itemId));
    }

    @PatchMapping("/files/{itemId}")
    public Result<DriveItemVo> updateFile(
            @PathVariable Long itemId,
            @Valid @RequestBody V21DriveFileUpdateRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(driveRuntimeBridgeService.updateFile(
                SecurityUtils.currentUserId(),
                itemId,
                request,
                clientIpResolver.resolve(httpRequest)
        ));
    }

    @DeleteMapping("/files/{itemId}")
    public Result<Void> deleteFile(@PathVariable Long itemId, HttpServletRequest httpRequest) {
        driveRuntimeBridgeService.deleteFile(SecurityUtils.currentUserId(), itemId, clientIpResolver.resolve(httpRequest));
        return Result.success(null);
    }

    @GetMapping("/files/{itemId}/share")
    public Result<List<DriveShareLinkVo>> shares(@PathVariable Long itemId) {
        return Result.success(driveRuntimeBridgeService.listShares(SecurityUtils.currentUserId(), itemId));
    }

    @PostMapping("/files/{itemId}/share")
    public Result<DriveShareLinkVo> createShare(
            @PathVariable Long itemId,
            @Valid @RequestBody CreateDriveShareRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(driveRuntimeBridgeService.createShare(
                SecurityUtils.currentUserId(),
                itemId,
                request,
                clientIpResolver.resolve(httpRequest)
        ));
    }

    @GetMapping("/storage/summary")
    public Result<DriveUsageVo> usage(HttpServletRequest httpRequest) {
        return Result.success(driveRuntimeBridgeService.usage(SecurityUtils.currentUserId(), clientIpResolver.resolve(httpRequest)));
    }
}
