package com.mmmail.server.controller;

import com.mmmail.common.model.Result;
import com.mmmail.server.model.vo.PassPublicSecureLinkVo;
import com.mmmail.server.service.PassBusinessService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/pass/secure-links")
public class PassPublicController {

    private final PassBusinessService passBusinessService;

    public PassPublicController(PassBusinessService passBusinessService) {
        this.passBusinessService = passBusinessService;
    }

    @GetMapping("/{token}")
    public Result<PassPublicSecureLinkVo> getSecureLink(@PathVariable String token, HttpServletRequest httpRequest) {
        return Result.success(passBusinessService.getPublicSecureLink(token, httpRequest.getRemoteAddr()));
    }
}
