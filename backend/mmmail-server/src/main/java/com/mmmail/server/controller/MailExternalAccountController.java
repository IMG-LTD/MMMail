package com.mmmail.server.controller;

import com.mmmail.common.model.Result;
import com.mmmail.server.model.dto.CreateMailExternalAccountRequest;
import com.mmmail.server.model.dto.UpdateMailExternalAccountRequest;
import com.mmmail.server.model.vo.MailExternalAccountSyncVo;
import com.mmmail.server.model.vo.MailExternalAccountTestVo;
import com.mmmail.server.model.vo.MailExternalAccountVo;
import com.mmmail.server.security.RequireEntitlement;
import com.mmmail.server.service.MailExternalAccountService;
import com.mmmail.server.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequireEntitlement("MAIL")
@RequestMapping("/api/v1/mail/external-accounts")
public class MailExternalAccountController {

    private final MailExternalAccountService mailExternalAccountService;

    public MailExternalAccountController(MailExternalAccountService mailExternalAccountService) {
        this.mailExternalAccountService = mailExternalAccountService;
    }

    @GetMapping
    public Result<List<MailExternalAccountVo>> list() {
        return Result.success(mailExternalAccountService.list(SecurityUtils.currentUserId()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Result<MailExternalAccountVo> create(@Valid @RequestBody CreateMailExternalAccountRequest request) {
        return Result.success(mailExternalAccountService.create(SecurityUtils.currentUserId(), request));
    }

    @GetMapping("/{accountId}")
    public Result<MailExternalAccountVo> get(@PathVariable Long accountId) {
        return Result.success(mailExternalAccountService.get(SecurityUtils.currentUserId(), accountId));
    }

    @PatchMapping("/{accountId}")
    public Result<MailExternalAccountVo> update(
            @PathVariable Long accountId,
            @Valid @RequestBody UpdateMailExternalAccountRequest request
    ) {
        return Result.success(mailExternalAccountService.update(SecurityUtils.currentUserId(), accountId, request));
    }

    @DeleteMapping("/{accountId}")
    public Result<Void> delete(@PathVariable Long accountId) {
        mailExternalAccountService.delete(SecurityUtils.currentUserId(), accountId);
        return Result.success(null);
    }

    @PostMapping("/{accountId}/test")
    public Result<MailExternalAccountTestVo> test(@PathVariable Long accountId) {
        return Result.success(mailExternalAccountService.test(SecurityUtils.currentUserId(), accountId));
    }

    @PostMapping("/{accountId}/sync")
    public Result<MailExternalAccountSyncVo> sync(@PathVariable Long accountId) {
        return Result.success(mailExternalAccountService.sync(SecurityUtils.currentUserId(), accountId));
    }
}
