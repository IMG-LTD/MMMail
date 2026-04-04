package com.mmmail.server.controller;

import com.mmmail.common.model.Result;
import com.mmmail.server.model.dto.BatchDriveItemsRequest;
import com.mmmail.server.model.dto.BatchCreateDriveShareRequest;
import com.mmmail.server.model.dto.CreateDriveFileRequest;
import com.mmmail.server.model.dto.CreateDriveFolderRequest;
import com.mmmail.server.model.dto.CreateEncryptedDriveShareRequest;
import com.mmmail.server.model.dto.CreateDriveShareRequest;
import com.mmmail.server.model.dto.MoveDriveItemRequest;
import com.mmmail.server.model.dto.RenameDriveItemRequest;
import com.mmmail.server.model.dto.SaveDriveSharedWithMeRequest;
import com.mmmail.server.model.dto.UploadDriveFileRequest;
import com.mmmail.server.model.dto.UpdateDriveShareRequest;
import com.mmmail.server.model.vo.DriveBatchActionResultVo;
import com.mmmail.server.model.vo.DriveBatchShareResultVo;
import com.mmmail.server.model.vo.DriveFileDownloadVo;
import com.mmmail.server.model.vo.DriveFilePreviewVo;
import com.mmmail.server.model.vo.DriveFileVersionVo;
import com.mmmail.server.model.vo.DriveItemVo;
import com.mmmail.server.model.vo.DriveShareAccessLogVo;
import com.mmmail.server.model.vo.DriveSavedShareVo;
import com.mmmail.server.model.vo.DriveShareLinkVo;
import com.mmmail.server.model.vo.DriveTrashItemVo;
import com.mmmail.server.model.vo.DriveUsageVo;
import com.mmmail.server.model.vo.DriveVersionCleanupVo;
import com.mmmail.server.service.DriveService;
import com.mmmail.server.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/v1/drive")
public class DriveController {

    private final DriveService driveService;

    public DriveController(DriveService driveService) {
        this.driveService = driveService;
    }

    @GetMapping("/items")
    public Result<List<DriveItemVo>> listItems(
            @RequestParam(required = false) Long parentId,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(required = false) String itemType,
            @RequestParam(required = false) Integer limit
    ) {
        return Result.success(driveService.listItems(SecurityUtils.currentUserId(), parentId, keyword, itemType, limit));
    }

    @PostMapping("/folders")
    public Result<DriveItemVo> createFolder(
            @Valid @RequestBody CreateDriveFolderRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(driveService.createFolder(SecurityUtils.currentUserId(), request, httpRequest.getRemoteAddr()));
    }

    @PostMapping("/files")
    public Result<DriveItemVo> createFile(
            @Valid @RequestBody CreateDriveFileRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(driveService.createFile(SecurityUtils.currentUserId(), request, httpRequest.getRemoteAddr()));
    }

    @PostMapping("/files/upload")
    public Result<DriveItemVo> uploadFile(
            @ModelAttribute UploadDriveFileRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(driveService.uploadFile(SecurityUtils.currentUserId(), request, httpRequest.getRemoteAddr()));
    }

    @GetMapping("/files/{itemId}/download")
    public ResponseEntity<ByteArrayResource> downloadOwnedFile(
            @PathVariable Long itemId,
            HttpServletRequest httpRequest
    ) {
        DriveFileDownloadVo file = driveService.downloadOwnedFile(SecurityUtils.currentUserId(), itemId, httpRequest.getRemoteAddr());
        return toFileResponse(file.fileName(), file.mimeType(), file.content(), false, false);
    }

    @GetMapping("/files/{itemId}/preview")
    public ResponseEntity<ByteArrayResource> previewOwnedFile(
            @PathVariable Long itemId,
            HttpServletRequest httpRequest
    ) {
        DriveFilePreviewVo file = driveService.previewOwnedFile(SecurityUtils.currentUserId(), itemId, httpRequest.getRemoteAddr());
        return toFileResponse(file.fileName(), file.mimeType(), file.content(), true, file.truncated());
    }

    @GetMapping("/files/{itemId}/versions")
    public Result<List<DriveFileVersionVo>> listFileVersions(
            @PathVariable Long itemId,
            @RequestParam(required = false) Integer limit
    ) {
        return Result.success(driveService.listFileVersions(SecurityUtils.currentUserId(), itemId, limit));
    }

    @PostMapping("/files/{itemId}/versions")
    public Result<DriveItemVo> uploadFileVersion(
            @PathVariable Long itemId,
            @ModelAttribute UploadDriveFileRequest uploadRequest,
            HttpServletRequest request
    ) {
        return Result.success(driveService.uploadFileVersion(
                SecurityUtils.currentUserId(),
                itemId,
                uploadRequest,
                request.getRemoteAddr()
        ));
    }

    @PostMapping("/files/{itemId}/versions/{versionId}/restore")
    public Result<DriveItemVo> restoreFileVersion(
            @PathVariable Long itemId,
            @PathVariable Long versionId,
            HttpServletRequest request
    ) {
        return Result.success(driveService.restoreFileVersion(SecurityUtils.currentUserId(), itemId, versionId, request.getRemoteAddr()));
    }

    @PostMapping("/files/{itemId}/versions/cleanup")
    public Result<DriveVersionCleanupVo> cleanupFileVersions(
            @PathVariable Long itemId,
            HttpServletRequest request
    ) {
        return Result.success(driveService.cleanupFileVersions(SecurityUtils.currentUserId(), itemId, request.getRemoteAddr()));
    }

    @PutMapping("/items/{itemId}/rename")
    public Result<DriveItemVo> renameItem(
            @PathVariable Long itemId,
            @Valid @RequestBody RenameDriveItemRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(driveService.renameItem(SecurityUtils.currentUserId(), itemId, request, httpRequest.getRemoteAddr()));
    }

    @PutMapping("/items/{itemId}/move")
    public Result<DriveItemVo> moveItem(
            @PathVariable Long itemId,
            @Valid @RequestBody MoveDriveItemRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(driveService.moveItem(SecurityUtils.currentUserId(), itemId, request, httpRequest.getRemoteAddr()));
    }

    @DeleteMapping("/items/{itemId}")
    public Result<Void> deleteItem(@PathVariable Long itemId, HttpServletRequest httpRequest) {
        driveService.deleteItem(SecurityUtils.currentUserId(), itemId, httpRequest.getRemoteAddr());
        return Result.success(null);
    }

    @PostMapping("/items/batch/delete")
    public Result<DriveBatchActionResultVo> batchDeleteItems(
            @Valid @RequestBody BatchDriveItemsRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(driveService.batchDeleteItems(SecurityUtils.currentUserId(), request, httpRequest.getRemoteAddr()));
    }

    @PostMapping("/items/batch/shares")
    public Result<DriveBatchShareResultVo> batchCreateShares(
            @Valid @RequestBody BatchCreateDriveShareRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(driveService.batchCreateShares(
                SecurityUtils.currentUserId(),
                request,
                httpRequest.getRemoteAddr()
        ));
    }

    @GetMapping("/trash/items")
    public Result<List<DriveTrashItemVo>> listTrashItems(@RequestParam(required = false) Integer limit) {
        return Result.success(driveService.listTrashItems(SecurityUtils.currentUserId(), limit));
    }

    @PostMapping("/trash/items/batch/restore")
    public Result<DriveBatchActionResultVo> batchRestoreTrashItems(
            @Valid @RequestBody BatchDriveItemsRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(driveService.batchRestoreTrashItems(SecurityUtils.currentUserId(), request, httpRequest.getRemoteAddr()));
    }

    @PostMapping("/trash/items/batch/purge")
    public Result<DriveBatchActionResultVo> batchPurgeTrashItems(
            @Valid @RequestBody BatchDriveItemsRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(driveService.batchPurgeTrashItems(SecurityUtils.currentUserId(), request, httpRequest.getRemoteAddr()));
    }

    @PostMapping("/trash/items/{itemId}/restore")
    public Result<DriveTrashItemVo> restoreTrashItem(@PathVariable Long itemId, HttpServletRequest request) {
        return Result.success(driveService.restoreTrashItem(SecurityUtils.currentUserId(), itemId, request.getRemoteAddr()));
    }

    @DeleteMapping("/trash/items/{itemId}")
    public Result<Void> purgeTrashItem(@PathVariable Long itemId, HttpServletRequest request) {
        driveService.purgeTrashItem(SecurityUtils.currentUserId(), itemId, request.getRemoteAddr());
        return Result.success(null);
    }

    @GetMapping("/shares/access-logs")
    public Result<List<DriveShareAccessLogVo>> listShareAccessLogs(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String accessStatus,
            @RequestParam(required = false) Integer limit
    ) {
        return Result.success(driveService.listShareAccessLogs(SecurityUtils.currentUserId(), action, accessStatus, limit));
    }

    @GetMapping("/items/{itemId}/shares")
    public Result<List<DriveShareLinkVo>> listShares(@PathVariable Long itemId) {
        return Result.success(driveService.listShares(SecurityUtils.currentUserId(), itemId));
    }

    @PostMapping("/items/{itemId}/shares")
    public Result<DriveShareLinkVo> createShare(
            @PathVariable Long itemId,
            @Valid @RequestBody CreateDriveShareRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(driveService.createShare(SecurityUtils.currentUserId(), itemId, request, httpRequest.getRemoteAddr()));
    }

    @PostMapping("/items/{itemId}/shares/e2ee")
    public Result<DriveShareLinkVo> createEncryptedShare(
            @PathVariable Long itemId,
            @Valid @ModelAttribute CreateEncryptedDriveShareRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(driveService.createEncryptedShare(
                SecurityUtils.currentUserId(),
                itemId,
                request,
                httpRequest.getRemoteAddr()
        ));
    }

    @PutMapping("/shares/{shareId}")
    public Result<DriveShareLinkVo> updateShare(
            @PathVariable Long shareId,
            @Valid @RequestBody UpdateDriveShareRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(driveService.updateShare(SecurityUtils.currentUserId(), shareId, request, httpRequest.getRemoteAddr()));
    }

    @PostMapping("/shares/{shareId}/revoke")
    public Result<DriveShareLinkVo> revokeShare(@PathVariable Long shareId, HttpServletRequest httpRequest) {
        return Result.success(driveService.revokeShare(SecurityUtils.currentUserId(), shareId, httpRequest.getRemoteAddr()));
    }

    @GetMapping("/shared-with-me")
    public Result<List<DriveSavedShareVo>> listSharedWithMe() {
        return Result.success(driveService.listSharedWithMe(SecurityUtils.currentUserId()));
    }

    @PostMapping("/shared-with-me")
    public Result<DriveSavedShareVo> saveSharedWithMe(
            @Valid @RequestBody SaveDriveSharedWithMeRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(driveService.saveSharedWithMe(
                SecurityUtils.currentUserId(),
                request,
                httpRequest.getRemoteAddr(),
                httpRequest.getHeader("User-Agent")
        ));
    }

    @DeleteMapping("/shared-with-me/{savedShareId}")
    public Result<Void> removeSharedWithMe(@PathVariable Long savedShareId, HttpServletRequest httpRequest) {
        driveService.removeSharedWithMe(SecurityUtils.currentUserId(), savedShareId, httpRequest.getRemoteAddr());
        return Result.success(null);
    }

    @GetMapping("/usage")
    public Result<DriveUsageVo> usage(HttpServletRequest httpRequest) {
        return Result.success(driveService.usage(SecurityUtils.currentUserId(), httpRequest.getRemoteAddr()));
    }

    private ResponseEntity<ByteArrayResource> toFileResponse(
            String fileName,
            String mimeType,
            byte[] content,
            boolean inline,
            boolean truncated
    ) {
        MediaType contentType = MediaType.APPLICATION_OCTET_STREAM;
        try {
            contentType = MediaType.parseMediaType(mimeType);
        } catch (Exception ignored) {
            // Fallback to octet-stream when mime type is malformed.
        }
        ContentDisposition disposition = (inline ? ContentDisposition.inline() : ContentDisposition.attachment())
                .filename(fileName, StandardCharsets.UTF_8)
                .build();
        ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.ok()
                .contentType(contentType)
                .contentLength(content.length)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString());
        if (inline) {
            responseBuilder.header("X-Preview-Truncated", String.valueOf(truncated));
        }
        return responseBuilder.body(new ByteArrayResource(content));
    }
}
