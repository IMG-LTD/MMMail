package com.mmmail.server.controller;

import com.mmmail.common.model.Result;
import com.mmmail.server.model.dto.CreateMailFilterRequest;
import com.mmmail.server.model.dto.PreviewMailFilterRequest;
import com.mmmail.server.model.dto.UpdateMailFilterRequest;
import com.mmmail.server.model.vo.MailFilterPreviewVo;
import com.mmmail.server.model.vo.MailFilterVo;
import com.mmmail.server.service.MailFilterService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/mail-filters")
public class MailFilterController {

    private final MailFilterService mailFilterService;
    private final MailService mailService;

    public MailFilterController(MailFilterService mailFilterService, MailService mailService) {
        this.mailFilterService = mailFilterService;
        this.mailService = mailService;
    }

    @GetMapping
    public Result<List<MailFilterVo>> list() {
        return Result.success(mailFilterService.list(SecurityUtils.currentUserId()));
    }

    @PostMapping
    public Result<MailFilterVo> create(
            @Valid @RequestBody CreateMailFilterRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(mailFilterService.create(
                SecurityUtils.currentUserId(),
                request,
                httpRequest.getRemoteAddr()
        ));
    }

    @PutMapping("/{filterId}")
    public Result<MailFilterVo> update(
            @PathVariable Long filterId,
            @Valid @RequestBody UpdateMailFilterRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(mailFilterService.update(
                SecurityUtils.currentUserId(),
                filterId,
                request,
                httpRequest.getRemoteAddr()
        ));
    }

    @DeleteMapping("/{filterId}")
    public Result<Void> delete(
            @PathVariable Long filterId,
            HttpServletRequest httpRequest
    ) {
        mailFilterService.delete(SecurityUtils.currentUserId(), filterId, httpRequest.getRemoteAddr());
        return Result.success(null);
    }

    @PostMapping("/preview")
    public Result<MailFilterPreviewVo> preview(
            @Valid @RequestBody PreviewMailFilterRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(mailService.previewMailFilter(
                SecurityUtils.currentUserId(),
                request,
                httpRequest.getRemoteAddr()
        ));
    }
}
