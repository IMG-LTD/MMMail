package com.mmmail.server.controller;

import com.mmmail.common.model.Result;
import com.mmmail.server.service.MailAttachmentService;
import com.mmmail.server.model.vo.MailPublicSecureLinkVo;
import com.mmmail.server.service.MailExternalSecureLinkService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/public/mail/secure-links")
public class MailPublicController {

    private final MailExternalSecureLinkService mailExternalSecureLinkService;

    public MailPublicController(MailExternalSecureLinkService mailExternalSecureLinkService) {
        this.mailExternalSecureLinkService = mailExternalSecureLinkService;
    }

    @GetMapping("/{token}")
    public Result<MailPublicSecureLinkVo> getSecureLink(@PathVariable String token, HttpServletRequest httpRequest) {
        return Result.success(mailExternalSecureLinkService.getPublicSecureLink(token, httpRequest.getRemoteAddr()));
    }

    @GetMapping("/{token}/attachments/{attachmentId}/download")
    public ResponseEntity<byte[]> downloadAttachment(
            @PathVariable String token,
            @PathVariable Long attachmentId,
            HttpServletRequest httpRequest
    ) {
        MailAttachmentService.PublicAttachmentDownload download = mailExternalSecureLinkService.downloadPublicAttachment(
                token,
                attachmentId,
                httpRequest.getRemoteAddr()
        );
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(download.contentType()));
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(download.fileName(), StandardCharsets.UTF_8)
                .build());
        return ResponseEntity.ok()
                .headers(headers)
                .body(download.bytes());
    }
}
