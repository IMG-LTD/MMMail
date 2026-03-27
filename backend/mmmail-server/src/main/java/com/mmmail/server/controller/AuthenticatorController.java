package com.mmmail.server.controller;

import com.mmmail.common.model.Result;
import com.mmmail.server.model.dto.CreateAuthenticatorBackupRequest;
import com.mmmail.server.model.dto.CreateAuthenticatorEntryRequest;
import com.mmmail.server.model.dto.ImportAuthenticatorQrImageRequest;
import com.mmmail.server.model.dto.ImportAuthenticatorBackupRequest;
import com.mmmail.server.model.dto.ImportAuthenticatorEntriesRequest;
import com.mmmail.server.model.dto.UpdateAuthenticatorSecurityRequest;
import com.mmmail.server.model.dto.UpdateAuthenticatorEntryRequest;
import com.mmmail.server.model.dto.VerifyAuthenticatorPinRequest;
import com.mmmail.server.model.vo.AuthenticatorBackupVo;
import com.mmmail.server.model.vo.AuthenticatorCodeVo;
import com.mmmail.server.model.vo.AuthenticatorEntryDetailVo;
import com.mmmail.server.model.vo.AuthenticatorExportVo;
import com.mmmail.server.model.vo.AuthenticatorImportResultVo;
import com.mmmail.server.model.vo.AuthenticatorEntrySummaryVo;
import com.mmmail.server.model.vo.AuthenticatorSecurityPinVerificationVo;
import com.mmmail.server.model.vo.AuthenticatorSecurityPreferenceVo;
import com.mmmail.server.service.AuthenticatorQrImageDecoder;
import com.mmmail.server.service.AuthenticatorSecurityPreferenceService;
import com.mmmail.server.service.AuthenticatorPortabilityService;
import com.mmmail.server.service.AuthenticatorService;
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
@RequestMapping("/api/v1/authenticator")
public class AuthenticatorController {

    private final AuthenticatorService authenticatorService;
    private final AuthenticatorPortabilityService portabilityService;
    private final AuthenticatorSecurityPreferenceService securityPreferenceService;
    private final AuthenticatorQrImageDecoder qrImageDecoder;

    public AuthenticatorController(
            AuthenticatorService authenticatorService,
            AuthenticatorPortabilityService portabilityService,
            AuthenticatorSecurityPreferenceService securityPreferenceService,
            AuthenticatorQrImageDecoder qrImageDecoder
    ) {
        this.authenticatorService = authenticatorService;
        this.portabilityService = portabilityService;
        this.securityPreferenceService = securityPreferenceService;
        this.qrImageDecoder = qrImageDecoder;
    }

    @GetMapping("/entries")
    public Result<List<AuthenticatorEntrySummaryVo>> list(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(required = false) Integer limit
    ) {
        return Result.success(authenticatorService.list(SecurityUtils.currentUserId(), keyword, limit));
    }

    @PostMapping("/entries")
    public Result<AuthenticatorEntryDetailVo> create(
            @Valid @RequestBody CreateAuthenticatorEntryRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(authenticatorService.create(
                SecurityUtils.currentUserId(),
                request.issuer(),
                request.accountName(),
                request.secretCiphertext(),
                request.algorithm(),
                request.digits(),
                request.periodSeconds(),
                httpRequest.getRemoteAddr()
        ));
    }

    @GetMapping("/entries/{entryId}")
    public Result<AuthenticatorEntryDetailVo> get(@PathVariable Long entryId) {
        return Result.success(authenticatorService.get(SecurityUtils.currentUserId(), entryId));
    }

    @PutMapping("/entries/{entryId}")
    public Result<AuthenticatorEntryDetailVo> update(
            @PathVariable Long entryId,
            @Valid @RequestBody UpdateAuthenticatorEntryRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(authenticatorService.update(
                SecurityUtils.currentUserId(),
                entryId,
                request.issuer(),
                request.accountName(),
                request.secretCiphertext(),
                request.algorithm(),
                request.digits(),
                request.periodSeconds(),
                httpRequest.getRemoteAddr()
        ));
    }

    @DeleteMapping("/entries/{entryId}")
    public Result<Void> delete(@PathVariable Long entryId, HttpServletRequest httpRequest) {
        authenticatorService.delete(SecurityUtils.currentUserId(), entryId, httpRequest.getRemoteAddr());
        return Result.success(null);
    }

    @PostMapping("/entries/{entryId}/code")
    public Result<AuthenticatorCodeVo> generateCode(@PathVariable Long entryId, HttpServletRequest httpRequest) {
        return Result.success(authenticatorService.generateCode(
                SecurityUtils.currentUserId(),
                entryId,
                httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/import")
    public Result<AuthenticatorImportResultVo> importEntries(
            @Valid @RequestBody ImportAuthenticatorEntriesRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(portabilityService.importEntries(
                SecurityUtils.currentUserId(),
                request,
                httpRequest.getRemoteAddr()
        ));
    }

    @GetMapping("/export")
    public Result<AuthenticatorExportVo> exportEntries(HttpServletRequest httpRequest) {
        return Result.success(portabilityService.exportEntries(
                SecurityUtils.currentUserId(),
                httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/backup/export")
    public Result<AuthenticatorBackupVo> exportEncryptedBackup(
            @Valid @RequestBody CreateAuthenticatorBackupRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(portabilityService.exportEncryptedBackup(
                SecurityUtils.currentUserId(),
                request,
                httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/backup/import")
    public Result<AuthenticatorImportResultVo> importEncryptedBackup(
            @Valid @RequestBody ImportAuthenticatorBackupRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(portabilityService.importEncryptedBackup(
                SecurityUtils.currentUserId(),
                request,
                httpRequest.getRemoteAddr()
        ));
    }

    @GetMapping("/security")
    public Result<AuthenticatorSecurityPreferenceVo> getSecurityPreference() {
        return Result.success(securityPreferenceService.get(SecurityUtils.currentUserId()));
    }

    @PutMapping("/security")
    public Result<AuthenticatorSecurityPreferenceVo> updateSecurityPreference(
            @Valid @RequestBody UpdateAuthenticatorSecurityRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(securityPreferenceService.update(
                SecurityUtils.currentUserId(),
                request,
                httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/security/verify-pin")
    public Result<AuthenticatorSecurityPinVerificationVo> verifyPin(
            @Valid @RequestBody VerifyAuthenticatorPinRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(securityPreferenceService.verifyPin(
                SecurityUtils.currentUserId(),
                request.pin(),
                httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/import/qr-image")
    public Result<AuthenticatorImportResultVo> importQrImage(
            @Valid @RequestBody ImportAuthenticatorQrImageRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(portabilityService.importEntries(
                SecurityUtils.currentUserId(),
                new ImportAuthenticatorEntriesRequest(qrImageDecoder.decodeOtpauthUri(request.dataUrl()), "OTPAUTH_URI"),
                httpRequest.getRemoteAddr()
        ));
    }
}
