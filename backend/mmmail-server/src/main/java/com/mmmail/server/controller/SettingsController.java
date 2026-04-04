package com.mmmail.server.controller;

import com.mmmail.common.model.Result;
import com.mmmail.server.model.dto.CreateBlockedSenderRequest;
import com.mmmail.server.model.dto.CreateBlockedDomainRequest;
import com.mmmail.server.model.dto.CreateTrustedSenderRequest;
import com.mmmail.server.model.dto.CreateTrustedDomainRequest;
import com.mmmail.server.model.dto.UpdateMailE2eeKeyProfileRequest;
import com.mmmail.server.model.dto.UpdateMailE2eeRecoveryRequest;
import com.mmmail.server.model.dto.UpdateProfileRequest;
import com.mmmail.server.model.vo.BlockedSenderVo;
import com.mmmail.server.model.vo.BlockedDomainVo;
import com.mmmail.server.model.vo.MailE2eeKeyProfileVo;
import com.mmmail.server.model.vo.MailE2eeRecoveryVo;
import com.mmmail.server.model.vo.TrustedSenderVo;
import com.mmmail.server.model.vo.TrustedDomainVo;
import com.mmmail.server.model.vo.RuleResolutionVo;
import com.mmmail.server.model.vo.UserPreferenceVo;
import com.mmmail.server.service.BlockedSenderService;
import com.mmmail.server.service.BlockedDomainService;
import com.mmmail.server.service.MailE2eeKeyProfileService;
import com.mmmail.server.service.MailE2eeRecoveryService;
import com.mmmail.server.service.MailService;
import com.mmmail.server.service.TrustedSenderService;
import com.mmmail.server.service.TrustedDomainService;
import com.mmmail.server.service.UserPreferenceService;
import com.mmmail.server.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Validated
@RequestMapping("/api/v1/settings")
public class SettingsController {

    private final UserPreferenceService userPreferenceService;
    private final MailE2eeKeyProfileService mailE2eeKeyProfileService;
    private final MailE2eeRecoveryService mailE2eeRecoveryService;
    private final BlockedSenderService blockedSenderService;
    private final TrustedSenderService trustedSenderService;
    private final BlockedDomainService blockedDomainService;
    private final TrustedDomainService trustedDomainService;
    private final MailService mailService;

    public SettingsController(
            UserPreferenceService userPreferenceService,
            MailE2eeKeyProfileService mailE2eeKeyProfileService,
            MailE2eeRecoveryService mailE2eeRecoveryService,
            BlockedSenderService blockedSenderService,
            TrustedSenderService trustedSenderService,
            BlockedDomainService blockedDomainService,
            TrustedDomainService trustedDomainService,
            MailService mailService
    ) {
        this.userPreferenceService = userPreferenceService;
        this.mailE2eeKeyProfileService = mailE2eeKeyProfileService;
        this.mailE2eeRecoveryService = mailE2eeRecoveryService;
        this.blockedSenderService = blockedSenderService;
        this.trustedSenderService = trustedSenderService;
        this.blockedDomainService = blockedDomainService;
        this.trustedDomainService = trustedDomainService;
        this.mailService = mailService;
    }

    @GetMapping("/profile")
    public Result<UserPreferenceVo> profile() {
        return Result.success(userPreferenceService.getProfile(SecurityUtils.currentUserId()));
    }

    @PutMapping("/profile")
    public Result<UserPreferenceVo> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(userPreferenceService.updateProfile(SecurityUtils.currentUserId(), request, httpRequest.getRemoteAddr()));
    }

    @GetMapping("/mail-e2ee")
    public Result<MailE2eeKeyProfileVo> mailE2ee(HttpServletRequest httpRequest) {
        return Result.success(mailE2eeKeyProfileService.get(SecurityUtils.currentUserId(), httpRequest.getRemoteAddr()));
    }

    @PutMapping("/mail-e2ee")
    public Result<MailE2eeKeyProfileVo> updateMailE2ee(
            @Valid @RequestBody UpdateMailE2eeKeyProfileRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(mailE2eeKeyProfileService.update(
                SecurityUtils.currentUserId(),
                request,
                httpRequest.getRemoteAddr()
        ));
    }

    @GetMapping("/mail-e2ee/recovery")
    public Result<MailE2eeRecoveryVo> mailE2eeRecovery(HttpServletRequest httpRequest) {
        return Result.success(mailE2eeRecoveryService.get(SecurityUtils.currentUserId(), httpRequest.getRemoteAddr()));
    }

    @PutMapping("/mail-e2ee/recovery")
    public Result<MailE2eeRecoveryVo> updateMailE2eeRecovery(
            @Valid @RequestBody UpdateMailE2eeRecoveryRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(mailE2eeRecoveryService.update(
                SecurityUtils.currentUserId(),
                request,
                httpRequest.getRemoteAddr()
        ));
    }

    @GetMapping("/blocked-senders")
    public Result<List<BlockedSenderVo>> blockedSenders() {
        return Result.success(blockedSenderService.listBlockedSenders(SecurityUtils.currentUserId()));
    }

    @PostMapping("/blocked-senders")
    public Result<BlockedSenderVo> addBlockedSender(
            @Valid @RequestBody CreateBlockedSenderRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(blockedSenderService.addBlockedSender(
                SecurityUtils.currentUserId(),
                request.email(),
                httpRequest.getRemoteAddr()
        ));
    }

    @DeleteMapping("/blocked-senders/{blockedSenderId}")
    public Result<Void> removeBlockedSender(
            @PathVariable Long blockedSenderId,
            HttpServletRequest httpRequest
    ) {
        blockedSenderService.removeBlockedSender(SecurityUtils.currentUserId(), blockedSenderId, httpRequest.getRemoteAddr());
        return Result.success(null);
    }

    @GetMapping("/trusted-senders")
    public Result<List<TrustedSenderVo>> trustedSenders() {
        return Result.success(trustedSenderService.listTrustedSenders(SecurityUtils.currentUserId()));
    }

    @PostMapping("/trusted-senders")
    public Result<TrustedSenderVo> addTrustedSender(
            @Valid @RequestBody CreateTrustedSenderRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(trustedSenderService.addTrustedSender(
                SecurityUtils.currentUserId(),
                request.email(),
                httpRequest.getRemoteAddr()
        ));
    }

    @DeleteMapping("/trusted-senders/{trustedSenderId}")
    public Result<Void> removeTrustedSender(
            @PathVariable Long trustedSenderId,
            HttpServletRequest httpRequest
    ) {
        trustedSenderService.removeTrustedSender(SecurityUtils.currentUserId(), trustedSenderId, httpRequest.getRemoteAddr());
        return Result.success(null);
    }

    @GetMapping("/blocked-domains")
    public Result<List<BlockedDomainVo>> blockedDomains() {
        return Result.success(blockedDomainService.listBlockedDomains(SecurityUtils.currentUserId()));
    }

    @PostMapping("/blocked-domains")
    public Result<BlockedDomainVo> addBlockedDomain(
            @Valid @RequestBody CreateBlockedDomainRequest request,
            HttpServletRequest httpRequest
    ) {
        Long userId = SecurityUtils.currentUserId();
        BlockedDomainVo domain = blockedDomainService.addBlockedDomain(userId, request.domain(), httpRequest.getRemoteAddr());
        trustedDomainService.removeTrustedDomainIfPresent(userId, request.domain());
        return Result.success(domain);
    }

    @DeleteMapping("/blocked-domains/{blockedDomainId}")
    public Result<Void> removeBlockedDomain(
            @PathVariable Long blockedDomainId,
            HttpServletRequest httpRequest
    ) {
        blockedDomainService.removeBlockedDomain(SecurityUtils.currentUserId(), blockedDomainId, httpRequest.getRemoteAddr());
        return Result.success(null);
    }

    @GetMapping("/trusted-domains")
    public Result<List<TrustedDomainVo>> trustedDomains() {
        return Result.success(trustedDomainService.listTrustedDomains(SecurityUtils.currentUserId()));
    }

    @PostMapping("/trusted-domains")
    public Result<TrustedDomainVo> addTrustedDomain(
            @Valid @RequestBody CreateTrustedDomainRequest request,
            HttpServletRequest httpRequest
    ) {
        Long userId = SecurityUtils.currentUserId();
        TrustedDomainVo domain = trustedDomainService.addTrustedDomain(userId, request.domain(), httpRequest.getRemoteAddr());
        blockedDomainService.removeBlockedDomainIfPresent(userId, request.domain());
        return Result.success(domain);
    }

    @DeleteMapping("/trusted-domains/{trustedDomainId}")
    public Result<Void> removeTrustedDomain(
            @PathVariable Long trustedDomainId,
            HttpServletRequest httpRequest
    ) {
        trustedDomainService.removeTrustedDomain(SecurityUtils.currentUserId(), trustedDomainId, httpRequest.getRemoteAddr());
        return Result.success(null);
    }

    @GetMapping("/rule-resolution")
    public Result<RuleResolutionVo> ruleResolution(
            @RequestParam @NotBlank @Email String senderEmail,
            HttpServletRequest httpRequest
    ) {
        return Result.success(mailService.resolveRuleResolution(
                SecurityUtils.currentUserId(),
                senderEmail,
                httpRequest.getRemoteAddr()
        ));
    }
}
