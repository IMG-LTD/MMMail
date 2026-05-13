package com.mmmail.server.controller;

import com.mmmail.common.model.Result;
import com.mmmail.server.model.vo.DriveFileDownloadVo;
import com.mmmail.server.model.vo.DriveItemVo;
import com.mmmail.server.model.vo.DrivePublicShareMetadataVo;
import com.mmmail.server.model.vo.MailPublicSecureLinkVo;
import com.mmmail.server.model.vo.PassPublicSecureLinkVo;
import com.mmmail.server.service.DriveService;
import com.mmmail.server.service.MailAttachmentService;
import com.mmmail.server.service.MailExternalSecureLinkService;
import com.mmmail.server.service.PassBusinessService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/v2/share")
public class V21PublicShareController {

    private static final String SHARE_PASSWORD_HEADER = "X-Drive-Share-Password";
    private static final String USER_AGENT_HEADER = "User-Agent";

    private final MailExternalSecureLinkService mailExternalSecureLinkService;
    private final DriveService driveService;
    private final PassBusinessService passBusinessService;

    public V21PublicShareController(
            MailExternalSecureLinkService mailExternalSecureLinkService,
            DriveService driveService,
            PassBusinessService passBusinessService
    ) {
        this.mailExternalSecureLinkService = mailExternalSecureLinkService;
        this.driveService = driveService;
        this.passBusinessService = passBusinessService;
    }

    @GetMapping("/mail/{token}")
    public Result<MailPublicSecureLinkVo> mailShare(@PathVariable String token, HttpServletRequest request) {
        return Result.success(mailExternalSecureLinkService.getPublicSecureLink(token, request.getRemoteAddr()));
    }

    @GetMapping("/mail/{token}/attachments/{attachmentId}/download")
    public ResponseEntity<byte[]> mailAttachment(
            @PathVariable String token,
            @PathVariable Long attachmentId,
            HttpServletRequest request
    ) {
        MailAttachmentService.PublicAttachmentDownload download = mailExternalSecureLinkService.downloadPublicAttachment(
                token,
                attachmentId,
                request.getRemoteAddr()
        );
        return mailAttachmentResponse(download);
    }

    @GetMapping("/drive/{token}")
    public Result<DrivePublicShareMetadataVo> driveMetadata(@PathVariable String token, HttpServletRequest request) {
        return Result.success(driveService.getPublicShareMetadataByToken(
                token,
                request.getRemoteAddr(),
                request.getHeader(USER_AGENT_HEADER)
        ));
    }

    @GetMapping("/drive/{token}/items")
    public Result<List<DriveItemVo>> driveItems(
            @PathVariable String token,
            @RequestParam(required = false) Long parentId,
            HttpServletRequest request
    ) {
        return Result.success(driveService.listPublicShareItems(
                token,
                parentId,
                request.getHeader(SHARE_PASSWORD_HEADER),
                request.getRemoteAddr(),
                request.getHeader(USER_AGENT_HEADER)
        ));
    }

    @GetMapping("/drive/{token}/items/{itemId}/download")
    public ResponseEntity<ByteArrayResource> driveItemDownload(
            @PathVariable String token,
            @PathVariable Long itemId,
            HttpServletRequest request
    ) {
        DriveFileDownloadVo file = driveService.downloadPublicShareItem(
                token,
                itemId,
                request.getHeader(SHARE_PASSWORD_HEADER),
                request.getRemoteAddr(),
                request.getHeader(USER_AGENT_HEADER)
        );
        return driveFileResponse(file);
    }

    @GetMapping("/pass/{token}")
    public Result<PassPublicSecureLinkVo> passShare(@PathVariable String token, HttpServletRequest request) {
        return Result.success(passBusinessService.getPublicSecureLink(token, request.getRemoteAddr()));
    }

    private static ResponseEntity<byte[]> mailAttachmentResponse(MailAttachmentService.PublicAttachmentDownload download) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(download.contentType()));
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(download.fileName(), StandardCharsets.UTF_8)
                .build());
        return ResponseEntity.ok()
                .headers(headers)
                .body(download.bytes());
    }

    private static ResponseEntity<ByteArrayResource> driveFileResponse(DriveFileDownloadVo file) {
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(file.fileName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.mimeType()))
                .contentLength(file.content().length)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(new ByteArrayResource(file.content()));
    }
}
