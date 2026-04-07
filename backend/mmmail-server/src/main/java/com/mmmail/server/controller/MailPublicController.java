package com.mmmail.server.controller;

import com.mmmail.common.model.Result;
import com.mmmail.server.model.vo.MailPublicSecureLinkVo;
import com.mmmail.server.service.MailExternalSecureLinkService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
