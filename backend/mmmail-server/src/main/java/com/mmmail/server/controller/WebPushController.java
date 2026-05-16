package com.mmmail.server.controller;

import com.mmmail.common.model.Result;
import com.mmmail.server.model.dto.RegisterWebPushSubscriptionRequest;
import com.mmmail.server.model.dto.WebPushTestDeliveryRequest;
import com.mmmail.server.model.vo.SuiteWebPushSubscriptionVo;
import com.mmmail.server.model.vo.WebPushPublicKeyVo;
import com.mmmail.server.model.vo.WebPushTestDeliveryVo;
import com.mmmail.server.security.RequireEntitlement;
import com.mmmail.server.service.WebPushService;
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
@RequireEntitlement("HOSTED")
@RequestMapping("/api/v1/web-push")
public class WebPushController {

    private final WebPushService webPushService;

    public WebPushController(WebPushService webPushService) {
        this.webPushService = webPushService;
    }

    @GetMapping("/vapid-public-key")
    public Result<WebPushPublicKeyVo> publicKey(HttpServletRequest request) {
        return Result.success(new WebPushPublicKeyVo(webPushService.getStatus(SecurityUtils.currentUserId(), request.getRemoteAddr()).vapidPublicKey()));
    }

    @GetMapping("/subscriptions")
    public Result<List<SuiteWebPushSubscriptionVo>> subscriptions(HttpServletRequest request) {
        return Result.success(webPushService.listSubscriptions(SecurityUtils.currentUserId(), request.getRemoteAddr()));
    }

    @PostMapping("/subscriptions")
    public Result<SuiteWebPushSubscriptionVo> register(
            @Valid @RequestBody RegisterWebPushSubscriptionRequest body,
            HttpServletRequest request
    ) {
        return Result.success(webPushService.registerSubscription(
                SecurityUtils.currentUserId(),
                body.toSuiteRequest(),
                body.label(),
                request.getRemoteAddr()
        ));
    }

    @DeleteMapping("/subscriptions/{subscriptionId}")
    public Result<Boolean> delete(@PathVariable Long subscriptionId, HttpServletRequest request) {
        return Result.success(webPushService.deleteSubscriptionById(SecurityUtils.currentUserId(), subscriptionId, request.getRemoteAddr()));
    }

    @PostMapping("/test")
    public Result<WebPushTestDeliveryVo> test(@RequestBody WebPushTestDeliveryRequest body, HttpServletRequest request) {
        Long subscriptionId = body == null ? null : body.subscriptionId();
        return Result.success(webPushService.testDelivery(SecurityUtils.currentUserId(), subscriptionId, request.getRemoteAddr()));
    }
}
