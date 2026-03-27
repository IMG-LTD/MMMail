package com.mmmail.server.controller;

import com.mmmail.common.model.Result;
import com.mmmail.server.model.dto.CreateOrgTeamSpaceFolderRequest;
import com.mmmail.server.model.dto.CreateOrgTeamSpaceRequest;
import com.mmmail.server.model.vo.DriveFileDownloadVo;
import com.mmmail.server.model.vo.OrgBusinessOverviewVo;
import com.mmmail.server.model.vo.OrgTeamSpaceItemVo;
import com.mmmail.server.model.vo.OrgTeamSpaceVo;
import com.mmmail.server.service.OrgBusinessService;
import com.mmmail.server.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/v1/orgs")
public class OrgBusinessController {

    private final OrgBusinessService orgBusinessService;

    public OrgBusinessController(OrgBusinessService orgBusinessService) {
        this.orgBusinessService = orgBusinessService;
    }

    @GetMapping("/{orgId}/business/overview")
    public Result<OrgBusinessOverviewVo> getBusinessOverview(
            @PathVariable Long orgId,
            HttpServletRequest httpRequest
    ) {
        return Result.success(orgBusinessService.getBusinessOverview(
                SecurityUtils.currentUserId(),
                orgId,
                httpRequest.getRemoteAddr()
        ));
    }

    @GetMapping("/{orgId}/team-spaces")
    public Result<List<OrgTeamSpaceVo>> listTeamSpaces(
            @PathVariable Long orgId,
            HttpServletRequest httpRequest
    ) {
        return Result.success(orgBusinessService.listTeamSpaces(
                SecurityUtils.currentUserId(),
                orgId,
                httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/{orgId}/team-spaces")
    public Result<OrgTeamSpaceVo> createTeamSpace(
            @PathVariable Long orgId,
            @Valid @RequestBody CreateOrgTeamSpaceRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(orgBusinessService.createTeamSpace(
                SecurityUtils.currentUserId(),
                orgId,
                request,
                httpRequest.getRemoteAddr()
        ));
    }

    @GetMapping("/{orgId}/team-spaces/{teamSpaceId}/items")
    public Result<List<OrgTeamSpaceItemVo>> listTeamSpaceItems(
            @PathVariable Long orgId,
            @PathVariable Long teamSpaceId,
            @RequestParam(required = false) Long parentId,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(required = false) String itemType,
            @RequestParam(required = false) Integer limit,
            HttpServletRequest httpRequest
    ) {
        return Result.success(orgBusinessService.listTeamSpaceItems(
                SecurityUtils.currentUserId(),
                orgId,
                teamSpaceId,
                parentId,
                keyword,
                itemType,
                limit,
                httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/{orgId}/team-spaces/{teamSpaceId}/folders")
    public Result<OrgTeamSpaceItemVo> createTeamSpaceFolder(
            @PathVariable Long orgId,
            @PathVariable Long teamSpaceId,
            @Valid @RequestBody CreateOrgTeamSpaceFolderRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(orgBusinessService.createFolder(
                SecurityUtils.currentUserId(),
                orgId,
                teamSpaceId,
                request,
                httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/{orgId}/team-spaces/{teamSpaceId}/files/upload")
    public Result<OrgTeamSpaceItemVo> uploadTeamSpaceFile(
            @PathVariable Long orgId,
            @PathVariable Long teamSpaceId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) Long parentId,
            HttpServletRequest httpRequest
    ) {
        return Result.success(orgBusinessService.uploadFile(
                SecurityUtils.currentUserId(),
                orgId,
                teamSpaceId,
                parentId,
                file,
                httpRequest.getRemoteAddr()
        ));
    }

    @GetMapping("/{orgId}/team-spaces/{teamSpaceId}/files/{itemId}/download")
    public ResponseEntity<ByteArrayResource> downloadTeamSpaceFile(
            @PathVariable Long orgId,
            @PathVariable Long teamSpaceId,
            @PathVariable Long itemId,
            HttpServletRequest httpRequest
    ) {
        DriveFileDownloadVo file = orgBusinessService.downloadFile(
                SecurityUtils.currentUserId(),
                orgId,
                teamSpaceId,
                itemId,
                httpRequest.getRemoteAddr()
        );
        return toFileResponse(file.fileName(), file.mimeType(), file.content());
    }

    private ResponseEntity<ByteArrayResource> toFileResponse(String fileName, String mimeType, byte[] content) {
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (mimeType != null && !mimeType.isBlank()) {
            try {
                mediaType = MediaType.parseMediaType(mimeType);
            } catch (Exception ignored) {
                mediaType = MediaType.APPLICATION_OCTET_STREAM;
            }
        }
        ByteArrayResource resource = new ByteArrayResource(content);
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(fileName, StandardCharsets.UTF_8)
                                .build()
                                .toString())
                .contentLength(content.length)
                .body(resource);
    }
}
