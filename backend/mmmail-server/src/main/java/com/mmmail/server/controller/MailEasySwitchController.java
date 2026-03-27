package com.mmmail.server.controller;

import com.mmmail.common.model.Result;
import com.mmmail.server.model.dto.CreateMailEasySwitchSessionRequest;
import com.mmmail.server.model.vo.MailEasySwitchSessionVo;
import com.mmmail.server.service.MailEasySwitchService;
import com.mmmail.server.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/mail-easy-switch/sessions")
public class MailEasySwitchController {

    private final MailEasySwitchService mailEasySwitchService;

    public MailEasySwitchController(MailEasySwitchService mailEasySwitchService) {
        this.mailEasySwitchService = mailEasySwitchService;
    }

    @GetMapping
    public Result<List<MailEasySwitchSessionVo>> list() {
        return Result.success(mailEasySwitchService.list(SecurityUtils.currentUserId()));
    }

    @PostMapping
    public Result<MailEasySwitchSessionVo> create(
            @Valid @RequestBody CreateMailEasySwitchSessionRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(mailEasySwitchService.create(SecurityUtils.currentUserId(), request, httpRequest.getRemoteAddr()));
    }

    @DeleteMapping("/{sessionId}")
    public Result<Void> delete(@PathVariable Long sessionId, HttpServletRequest httpRequest) {
        mailEasySwitchService.delete(SecurityUtils.currentUserId(), sessionId, httpRequest.getRemoteAddr());
        return Result.success(null);
    }
}
