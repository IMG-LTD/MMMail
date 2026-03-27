package com.mmmail.server.controller;

import com.mmmail.common.model.Result;
import com.mmmail.server.model.dto.CreateMailFolderRequest;
import com.mmmail.server.model.dto.UpdateMailFolderRequest;
import com.mmmail.server.model.vo.MailFolderNodeVo;
import com.mmmail.server.model.vo.MailPageVo;
import com.mmmail.server.service.MailFolderService;
import com.mmmail.server.service.MailService;
import com.mmmail.server.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/mail-folders")
public class MailFolderController {

    private final MailFolderService mailFolderService;
    private final MailService mailService;

    public MailFolderController(MailFolderService mailFolderService, MailService mailService) {
        this.mailFolderService = mailFolderService;
        this.mailService = mailService;
    }

    @GetMapping
    public Result<List<MailFolderNodeVo>> list(HttpServletRequest httpRequest) {
        return Result.success(mailFolderService.list(SecurityUtils.currentUserId(), httpRequest.getRemoteAddr()));
    }

    @PostMapping
    public Result<MailFolderNodeVo> create(
            @Valid @RequestBody CreateMailFolderRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(mailFolderService.create(
                SecurityUtils.currentUserId(),
                request,
                httpRequest.getRemoteAddr()
        ));
    }

    @PutMapping("/{folderId}")
    public Result<MailFolderNodeVo> update(
            @PathVariable Long folderId,
            @Valid @RequestBody UpdateMailFolderRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(mailFolderService.update(
                SecurityUtils.currentUserId(),
                folderId,
                request,
                httpRequest.getRemoteAddr()
        ));
    }

    @DeleteMapping("/{folderId}")
    public Result<Void> delete(@PathVariable Long folderId, HttpServletRequest httpRequest) {
        mailFolderService.delete(SecurityUtils.currentUserId(), folderId, httpRequest.getRemoteAddr());
        return Result.success(null);
    }

    @GetMapping("/{folderId}/messages")
    public Result<MailPageVo> messages(
            @PathVariable Long folderId,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(defaultValue = "") String keyword
    ) {
        return Result.success(mailService.listCustomFolder(SecurityUtils.currentUserId(), folderId, page, size, keyword));
    }
}
