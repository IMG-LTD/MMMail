package com.mmmail.server.controller;

import com.mmmail.common.model.Result;
import com.mmmail.server.model.vo.DriveFileDownloadVo;
import com.mmmail.server.model.vo.DriveFilePreviewVo;
import com.mmmail.server.model.vo.DriveItemVo;
import com.mmmail.server.model.vo.DrivePublicShareMetadataVo;
import com.mmmail.server.service.DriveService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/v1/public/drive/shares")
public class DrivePublicController {

    private static final String PUBLIC_SHARE_CAPABILITY_PATH = "/api/v2/public-share/capabilities";
    private static final String SHARE_PASSWORD_HEADER = "X-Drive-Share-Password";

    private final DriveService driveService;

    public DrivePublicController(DriveService driveService) {
        this.driveService = driveService;
    }

    @GetMapping("/{token}/metadata")
    public Result<DrivePublicShareMetadataVo> metadata(@PathVariable String token, HttpServletRequest request) {
        return Result.success(
                driveService.getPublicShareMetadataByToken(token, request.getRemoteAddr(), request.getHeader("User-Agent"))
        );
    }

    @GetMapping("/{token}/items")
    public Result<List<DriveItemVo>> listItems(
            @PathVariable String token,
            @RequestParam(required = false) Long parentId,
            HttpServletRequest request
    ) {
        return Result.success(
                driveService.listPublicShareItems(
                        token,
                        parentId,
                        request.getHeader(SHARE_PASSWORD_HEADER),
                        request.getRemoteAddr(),
                        request.getHeader("User-Agent")
                )
        );
    }

    @GetMapping("/{token}/download")
    public ResponseEntity<ByteArrayResource> download(@PathVariable String token, HttpServletRequest request) {
        DriveFileDownloadVo file = driveService.downloadByPublicToken(
                token,
                request.getHeader(SHARE_PASSWORD_HEADER),
                request.getRemoteAddr(),
                request.getHeader("User-Agent")
        );
        return toFileResponse(file.fileName(), file.mimeType(), file.content(), false, false);
    }

    @GetMapping("/{token}/preview")
    public ResponseEntity<ByteArrayResource> preview(@PathVariable String token, HttpServletRequest request) {
        DriveFilePreviewVo file = driveService.previewByPublicToken(
                token,
                request.getHeader(SHARE_PASSWORD_HEADER),
                request.getRemoteAddr(),
                request.getHeader("User-Agent")
        );
        return toFileResponse(file.fileName(), file.mimeType(), file.content(), true, file.truncated());
    }

    @GetMapping("/{token}/items/{itemId}/download")
    public ResponseEntity<ByteArrayResource> downloadItem(
            @PathVariable String token,
            @PathVariable Long itemId,
            HttpServletRequest request
    ) {
        DriveFileDownloadVo file = driveService.downloadPublicShareItem(
                token,
                itemId,
                request.getHeader(SHARE_PASSWORD_HEADER),
                request.getRemoteAddr(),
                request.getHeader("User-Agent")
        );
        return toFileResponse(file.fileName(), file.mimeType(), file.content(), false, false);
    }

    @GetMapping("/{token}/items/{itemId}/preview")
    public ResponseEntity<ByteArrayResource> previewItem(
            @PathVariable String token,
            @PathVariable Long itemId,
            HttpServletRequest request
    ) {
        DriveFilePreviewVo file = driveService.previewPublicShareItem(
                token,
                itemId,
                request.getHeader(SHARE_PASSWORD_HEADER),
                request.getRemoteAddr(),
                request.getHeader("User-Agent")
        );
        return toFileResponse(file.fileName(), file.mimeType(), file.content(), true, file.truncated());
    }

    @PostMapping("/{token}/files/upload")
    public Result<DriveItemVo> uploadFile(
            @PathVariable String token,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) Long parentId,
            HttpServletRequest request
    ) {
        return Result.success(
                driveService.uploadToPublicShareFolder(
                        token,
                        parentId,
                        request.getHeader(SHARE_PASSWORD_HEADER),
                        file,
                        request.getRemoteAddr(),
                        request.getHeader("User-Agent")
                )
        );
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
