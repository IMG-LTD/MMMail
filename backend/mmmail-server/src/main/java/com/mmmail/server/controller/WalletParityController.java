package com.mmmail.server.controller;

import com.mmmail.common.model.Result;
import com.mmmail.server.model.dto.CreateWalletEmailTransferRequest;
import com.mmmail.server.model.dto.ImportWalletAccountRequest;
import com.mmmail.server.model.dto.RotateWalletReceiveAddressRequest;
import com.mmmail.server.model.dto.UpdateWalletAdvancedSettingsRequest;
import com.mmmail.server.model.dto.UpdateWalletAccountProfileRequest;
import com.mmmail.server.model.vo.WalletAccountProfileVo;
import com.mmmail.server.model.vo.WalletAddressBookVo;
import com.mmmail.server.model.vo.WalletAdvancedSettingsVo;
import com.mmmail.server.model.vo.WalletEmailTransferVo;
import com.mmmail.server.model.vo.WalletParityWorkspaceVo;
import com.mmmail.server.model.vo.WalletReceiveAddressVo;
import com.mmmail.server.model.vo.WalletRecoveryPhraseVo;
import com.mmmail.server.service.WalletParityService;
import com.mmmail.server.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/wallet")
public class WalletParityController {

    private final WalletParityService walletParityService;

    public WalletParityController(WalletParityService walletParityService) {
        this.walletParityService = walletParityService;
    }

    @GetMapping("/accounts/{accountId}/parity-workspace")
    public Result<WalletParityWorkspaceVo> workspace(
            @PathVariable Long accountId,
            @RequestParam(required = false) Integer limit,
            HttpServletRequest httpRequest
    ) {
        return Result.success(walletParityService.getWorkspace(
                SecurityUtils.currentUserId(),
                accountId,
                limit,
                httpRequest.getRemoteAddr()
        ));
    }

    @GetMapping("/accounts/{accountId}/address-book")
    public Result<WalletAddressBookVo> addressBook(
            @PathVariable Long accountId,
            @RequestParam(required = false) String kind,
            @RequestParam(required = false) String query,
            HttpServletRequest httpRequest
    ) {
        return Result.success(walletParityService.getAddressBook(
                SecurityUtils.currentUserId(),
                accountId,
                kind,
                query,
                httpRequest.getRemoteAddr()
        ));
    }

    @PutMapping("/accounts/{accountId}/parity-profile")
    public Result<WalletAccountProfileVo> updateProfile(
            @PathVariable Long accountId,
            @Valid @RequestBody UpdateWalletAccountProfileRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(walletParityService.updateProfile(
                SecurityUtils.currentUserId(),
                accountId,
                request.bitcoinViaEmailEnabled(),
                request.aliasEmail(),
                request.balanceMasked(),
                request.addressPrivacyEnabled(),
                request.addressPoolSize(),
                request.passphraseHint(),
                httpRequest.getRemoteAddr()
        ));
    }

    @PutMapping("/accounts/{accountId}/advanced-settings")
    public Result<WalletAdvancedSettingsVo> updateAdvancedSettings(
            @PathVariable Long accountId,
            @Valid @RequestBody UpdateWalletAdvancedSettingsRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(walletParityService.updateAdvancedSettings(
                SecurityUtils.currentUserId(),
                accountId,
                request,
                httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/accounts/import")
    public Result<WalletAdvancedSettingsVo> importWallet(
            @Valid @RequestBody ImportWalletAccountRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(walletParityService.importWallet(
                SecurityUtils.currentUserId(),
                request,
                httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/accounts/{accountId}/receive-addresses/rotate")
    public Result<WalletReceiveAddressVo> rotateReceiveAddress(
            @PathVariable Long accountId,
            @Valid @RequestBody(required = false) RotateWalletReceiveAddressRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(walletParityService.rotateReceiveAddress(
                SecurityUtils.currentUserId(),
                accountId,
                request == null ? null : request.label(),
                httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/accounts/{accountId}/email-transfers")
    public Result<WalletEmailTransferVo> createEmailTransfer(
            @PathVariable Long accountId,
            @Valid @RequestBody CreateWalletEmailTransferRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(walletParityService.createEmailTransfer(
                SecurityUtils.currentUserId(),
                accountId,
                request.amountMinor(),
                request.recipientEmail(),
                request.deliveryMessage(),
                request.memo(),
                httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/email-transfers/{transferId}/claim")
    public Result<WalletEmailTransferVo> claimEmailTransfer(
            @PathVariable Long transferId,
            HttpServletRequest httpRequest
    ) {
        return Result.success(walletParityService.claimEmailTransfer(
                SecurityUtils.currentUserId(),
                transferId,
                httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/accounts/{accountId}/recovery/reveal")
    public Result<WalletRecoveryPhraseVo> revealRecoveryPhrase(
            @PathVariable Long accountId,
            HttpServletRequest httpRequest
    ) {
        return Result.success(walletParityService.revealRecoveryPhrase(
                SecurityUtils.currentUserId(),
                accountId,
                httpRequest.getRemoteAddr()
        ));
    }
}
