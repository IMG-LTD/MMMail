package com.mmmail.server.controller;

import com.mmmail.common.model.Result;
import com.mmmail.server.model.dto.CreateDriveCollaboratorShareRequest;
import com.mmmail.server.model.dto.CreateDriveFolderRequest;
import com.mmmail.server.model.dto.RespondDriveCollaboratorShareRequest;
import com.mmmail.server.model.dto.UpdateDriveCollaboratorShareRequest;
import com.mmmail.server.model.vo.DriveCollaboratorShareVo;
import com.mmmail.server.model.vo.DriveCollaboratorSharedItemVo;
import com.mmmail.server.model.vo.DriveFileDownloadVo;
import com.mmmail.server.model.vo.DriveFilePreviewVo;
import com.mmmail.server.model.vo.DriveIncomingCollaboratorShareVo;
import com.mmmail.server.model.vo.DriveItemVo;
import com.mmmail.server.service.DriveCollaborationService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/v1/drive")
public class DriveCollaborationController {

    private final DriveCollaborationService driveCollaborationService;

    public DriveCollaborationController(DriveCollaborationService driveCollaborationService) {
        this.driveCollaborationService = driveCollaborationService;
    }

    @GetMapping("/items/{itemId}/collaborator-shares")
    public Result<List<DriveCollaboratorShareVo>> listShares(@PathVariable Long itemId) {
        return Result.success(driveCollaborationService.listShares(SecurityUtils.currentUserId(), itemId));
    }

    @PostMapping("/items/{itemId}/collaborator-shares")
    public Result<DriveCollaboratorShareVo> createShare(
            @PathVariable Long itemId,
            @Valid @RequestBody CreateDriveCollaboratorShareRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(driveCollaborationService.createShare(
                SecurityUtils.currentUserId(),
                itemId,
                request,
                httpRequest.getRemoteAddr()
        ));
    }

    @PutMapping("/items/{itemId}/collaborator-shares/{shareId}")
    public Result<DriveCollaboratorShareVo> updateShare(
            @PathVariable Long itemId,
            @PathVariable Long shareId,
            @Valid @RequestBody UpdateDriveCollaboratorShareRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(driveCollaborationService.updateShare(
                SecurityUtils.currentUserId(),
                itemId,
                shareId,
                request,
                httpRequest.getRemoteAddr()
        ));
    }

    @DeleteMapping("/items/{itemId}/collaborator-shares/{shareId}")
    public Result<Void> removeShare(
            @PathVariable Long itemId,
            @PathVariable Long shareId,
            HttpServletRequest httpRequest
    ) {
        driveCollaborationService.removeShare(SecurityUtils.currentUserId(), itemId, shareId, httpRequest.getRemoteAddr());
        return Result.success(null);
    }

    @GetMapping("/collaborator-shares/incoming")
    public Result<List<DriveIncomingCollaboratorShareVo>> listIncomingShares(HttpServletRequest httpRequest) {
        return Result.success(driveCollaborationService.listIncomingShares(SecurityUtils.currentUserId(), httpRequest.getRemoteAddr()));
    }

    @PostMapping("/collaborator-shares/{shareId}/respond")
    public Result<DriveIncomingCollaboratorShareVo> respondShare(
            @PathVariable Long shareId,
            @Valid @RequestBody RespondDriveCollaboratorShareRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(driveCollaborationService.respondShare(
                SecurityUtils.currentUserId(),
                shareId,
                request,
                httpRequest.getRemoteAddr()
        ));
    }

    @GetMapping("/collaborator-shares/shared-with-me")
    public Result<List<DriveCollaboratorSharedItemVo>> listSharedWithMe(HttpServletRequest httpRequest) {
        return Result.success(driveCollaborationService.listSharedWithMe(SecurityUtils.currentUserId(), httpRequest.getRemoteAddr()));
    }

    @GetMapping("/collaborator-shares/{shareId}/items")
    public Result<List<DriveItemVo>> listSharedItems(
            @PathVariable Long shareId,
            @RequestParam(required = false) Long parentId,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(required = false) String itemType,
            @RequestParam(required = false) Integer limit
    ) {
        return Result.success(driveCollaborationService.listSharedItems(
                SecurityUtils.currentUserId(),
                shareId,
                parentId,
                keyword,
                itemType,
                limit
        ));
    }

    @PostMapping("/collaborator-shares/{shareId}/folders")
    public Result<DriveItemVo> createFolder(
            @PathVariable Long shareId,
            @Valid @RequestBody CreateDriveFolderRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(driveCollaborationService.createFolder(
                SecurityUtils.currentUserId(),
                shareId,
                request,
                httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/collaborator-shares/{shareId}/files/upload")
    public Result<DriveItemVo> uploadFile(
            @PathVariable Long shareId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) Long parentId,
            HttpServletRequest httpRequest
    ) {
        return Result.success(driveCollaborationService.uploadFile(
                SecurityUtils.currentUserId(),
                shareId,
                parentId,
                file,
                httpRequest.getRemoteAddr()
        ));
    }

    @GetMapping("/collaborator-shares/{shareId}/files/{itemId}/download")
    public ResponseEntity<ByteArrayResource> downloadFile(
            @PathVariable Long shareId,
            @PathVariable Long itemId,
            HttpServletRequest httpRequest
    ) {
        DriveFileDownloadVo file = driveCollaborationService.downloadFile(
                SecurityUtils.currentUserId(),
                shareId,
                itemId,
                httpRequest.getRemoteAddr()
        );
        return toFileResponse(file.fileName(), file.mimeType(), file.content(), false, false);
    }

    @GetMapping("/collaborator-shares/{shareId}/files/{itemId}/preview")
    public ResponseEntity<ByteArrayResource> previewFile(
            @PathVariable Long shareId,
            @PathVariable Long itemId,
            HttpServletRequest httpRequest
    ) {
        DriveFilePreviewVo file = driveCollaborationService.previewFile(
                SecurityUtils.currentUserId(),
                shareId,
                itemId,
                httpRequest.getRemoteAddr()
        );
        return toFileResponse(file.fileName(), file.mimeType(), file.content(), true, file.truncated());
    }

    private ResponseEntity<ByteArrayResource> toFileResponse(
            String fileName,
            String mimeType,
            byte[] content,
            boolean inline,
            boolean truncated
    ) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.builder(inline ? "inline" : "attachment")
                .filename(fileName, StandardCharsets.UTF_8)
                .build());
        headers.setContentLength(content.length);
        headers.add("X-Preview-Truncated", String.valueOf(truncated));
        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(mimeType);
        } catch (Exception ignored) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(mediaType)
                .body(new ByteArrayResource(content));
    }
}
