package com.mmmail.server.controller;

import com.mmmail.common.model.Result;
import com.mmmail.server.model.dto.AddPassSharedVaultMemberRequest;
import com.mmmail.server.model.dto.CreatePassItemRequest;
import com.mmmail.server.model.dto.CreatePassItemShareRequest;
import com.mmmail.server.model.dto.CreatePassSecureLinkRequest;
import com.mmmail.server.model.dto.CreatePassSharedVaultRequest;
import com.mmmail.server.model.dto.UpsertPassItemTwoFactorRequest;
import com.mmmail.server.model.dto.UpdatePassBusinessPolicyRequest;
import com.mmmail.server.model.dto.UpdatePassItemRequest;
import com.mmmail.server.model.vo.AuthenticatorCodeVo;
import com.mmmail.server.model.vo.OrgAuditEventVo;
import com.mmmail.server.model.vo.PassBusinessOverviewVo;
import com.mmmail.server.model.vo.PassBusinessPolicyVo;
import com.mmmail.server.model.vo.PassIncomingSharedItemDetailVo;
import com.mmmail.server.model.vo.PassIncomingSharedItemSummaryVo;
import com.mmmail.server.model.vo.PassItemDetailVo;
import com.mmmail.server.model.vo.PassItemShareVo;
import com.mmmail.server.model.vo.PassItemSummaryVo;
import com.mmmail.server.model.vo.PassMonitorOverviewVo;
import com.mmmail.server.model.vo.PassSecureLinkDashboardVo;
import com.mmmail.server.model.vo.PassSecureLinkVo;
import com.mmmail.server.model.vo.PassSharedVaultMemberVo;
import com.mmmail.server.model.vo.PassSharedVaultSummaryVo;
import com.mmmail.server.service.PassBusinessService;
import com.mmmail.server.service.PassItemShareService;
import com.mmmail.server.service.PassMonitorService;
import com.mmmail.server.service.PublicBaseUrlResolver;
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
@RequestMapping("/api/v1/pass")
public class PassBusinessController {

    private final PassBusinessService passBusinessService;
    private final PassItemShareService passItemShareService;
    private final PassMonitorService passMonitorService;
    private final PublicBaseUrlResolver publicBaseUrlResolver;

    public PassBusinessController(
            PassBusinessService passBusinessService,
            PassItemShareService passItemShareService,
            PassMonitorService passMonitorService,
            PublicBaseUrlResolver publicBaseUrlResolver
    ) {
        this.passBusinessService = passBusinessService;
        this.passItemShareService = passItemShareService;
        this.passMonitorService = passMonitorService;
        this.publicBaseUrlResolver = publicBaseUrlResolver;
    }

    @GetMapping("/orgs/{orgId}/overview")
    public Result<PassBusinessOverviewVo> getOverview(@PathVariable Long orgId, HttpServletRequest httpRequest) {
        return Result.success(passBusinessService.getOverview(SecurityUtils.currentUserId(), orgId, httpRequest.getRemoteAddr()));
    }

    @GetMapping("/orgs/{orgId}/monitor")
    public Result<PassMonitorOverviewVo> getMonitor(@PathVariable Long orgId, HttpServletRequest httpRequest) {
        return Result.success(passMonitorService.getSharedMonitor(SecurityUtils.currentUserId(), orgId, httpRequest.getRemoteAddr()));
    }

    @GetMapping("/orgs/{orgId}/policy")
    public Result<PassBusinessPolicyVo> getPolicy(@PathVariable Long orgId, HttpServletRequest httpRequest) {
        return Result.success(passBusinessService.getPolicy(SecurityUtils.currentUserId(), orgId, httpRequest.getRemoteAddr()));
    }

    @PutMapping("/orgs/{orgId}/policy")
    public Result<PassBusinessPolicyVo> updatePolicy(
            @PathVariable Long orgId,
            @Valid @RequestBody UpdatePassBusinessPolicyRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(passBusinessService.updatePolicy(
                SecurityUtils.currentUserId(),
                orgId,
                request,
                httpRequest.getRemoteAddr()
        ));
    }

    @GetMapping("/orgs/{orgId}/shared-vaults")
    public Result<List<PassSharedVaultSummaryVo>> listSharedVaults(
            @PathVariable Long orgId,
            @RequestParam(required = false) String keyword,
            HttpServletRequest httpRequest
    ) {
        return Result.success(passBusinessService.listSharedVaults(
                SecurityUtils.currentUserId(),
                orgId,
                keyword,
                httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/orgs/{orgId}/shared-vaults")
    public Result<PassSharedVaultSummaryVo> createSharedVault(
            @PathVariable Long orgId,
            @Valid @RequestBody CreatePassSharedVaultRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(passBusinessService.createSharedVault(
                SecurityUtils.currentUserId(),
                orgId,
                request,
                httpRequest.getRemoteAddr()
        ));
    }

    @GetMapping("/orgs/{orgId}/shared-vaults/{vaultId}/members")
    public Result<List<PassSharedVaultMemberVo>> listMembers(
            @PathVariable Long orgId,
            @PathVariable Long vaultId,
            HttpServletRequest httpRequest
    ) {
        return Result.success(passBusinessService.listSharedVaultMembers(
                SecurityUtils.currentUserId(),
                orgId,
                vaultId,
                httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/orgs/{orgId}/shared-vaults/{vaultId}/members")
    public Result<PassSharedVaultMemberVo> addMember(
            @PathVariable Long orgId,
            @PathVariable Long vaultId,
            @Valid @RequestBody AddPassSharedVaultMemberRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(passBusinessService.addSharedVaultMember(
                SecurityUtils.currentUserId(),
                orgId,
                vaultId,
                request,
                httpRequest.getRemoteAddr()
        ));
    }

    @DeleteMapping("/orgs/{orgId}/shared-vaults/{vaultId}/members/{memberId}")
    public Result<Void> removeMember(
            @PathVariable Long orgId,
            @PathVariable Long vaultId,
            @PathVariable Long memberId,
            HttpServletRequest httpRequest
    ) {
        passBusinessService.removeSharedVaultMember(
                SecurityUtils.currentUserId(),
                orgId,
                vaultId,
                memberId,
                httpRequest.getRemoteAddr()
        );
        return Result.success(null);
    }

    @GetMapping("/orgs/{orgId}/shared-vaults/{vaultId}/items")
    public Result<List<PassItemSummaryVo>> listSharedItems(
            @PathVariable Long orgId,
            @PathVariable Long vaultId,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "false") Boolean favoriteOnly,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String itemType,
            HttpServletRequest httpRequest
    ) {
        return Result.success(passBusinessService.listSharedItems(
                SecurityUtils.currentUserId(),
                orgId,
                vaultId,
                keyword,
                favoriteOnly,
                limit,
                itemType,
                httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/orgs/{orgId}/shared-vaults/{vaultId}/items")
    public Result<PassItemDetailVo> createSharedItem(
            @PathVariable Long orgId,
            @PathVariable Long vaultId,
            @Valid @RequestBody CreatePassItemRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(passBusinessService.createSharedItem(
                SecurityUtils.currentUserId(),
                orgId,
                vaultId,
                request,
                httpRequest.getRemoteAddr()
        ));
    }

    @GetMapping("/orgs/{orgId}/shared-vaults/{vaultId}/items/{itemId}")
    public Result<PassItemDetailVo> getSharedItem(
            @PathVariable Long orgId,
            @PathVariable Long vaultId,
            @PathVariable Long itemId,
            HttpServletRequest httpRequest
    ) {
        return Result.success(passBusinessService.getSharedItem(
                SecurityUtils.currentUserId(),
                orgId,
                vaultId,
                itemId,
                httpRequest.getRemoteAddr()
        ));
    }

    @GetMapping("/orgs/{orgId}/items/{itemId}/item-shares")
    public Result<List<PassItemShareVo>> listItemShares(
            @PathVariable Long orgId,
            @PathVariable Long itemId,
            HttpServletRequest httpRequest
    ) {
        return Result.success(passItemShareService.listItemShares(
                SecurityUtils.currentUserId(),
                orgId,
                itemId,
                httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/orgs/{orgId}/items/{itemId}/item-shares")
    public Result<PassItemShareVo> createItemShare(
            @PathVariable Long orgId,
            @PathVariable Long itemId,
            @Valid @RequestBody CreatePassItemShareRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(passItemShareService.createItemShare(
                SecurityUtils.currentUserId(),
                orgId,
                itemId,
                request,
                httpRequest.getRemoteAddr()
        ));
    }

    @DeleteMapping("/orgs/{orgId}/items/{itemId}/item-shares/{shareId}")
    public Result<Void> removeItemShare(
            @PathVariable Long orgId,
            @PathVariable Long itemId,
            @PathVariable Long shareId,
            HttpServletRequest httpRequest
    ) {
        passItemShareService.removeItemShare(
                SecurityUtils.currentUserId(),
                orgId,
                itemId,
                shareId,
                httpRequest.getRemoteAddr()
        );
        return Result.success(null);
    }

    @PostMapping("/orgs/{orgId}/items/{itemId}/monitor/exclude")
    public Result<Void> excludeSharedItemFromMonitor(
            @PathVariable Long orgId,
            @PathVariable Long itemId,
            HttpServletRequest httpRequest
    ) {
        passMonitorService.setSharedMonitorExcluded(SecurityUtils.currentUserId(), orgId, itemId, true, httpRequest.getRemoteAddr());
        return Result.success(null);
    }

    @DeleteMapping("/orgs/{orgId}/items/{itemId}/monitor/exclude")
    public Result<Void> includeSharedItemInMonitor(
            @PathVariable Long orgId,
            @PathVariable Long itemId,
            HttpServletRequest httpRequest
    ) {
        passMonitorService.setSharedMonitorExcluded(SecurityUtils.currentUserId(), orgId, itemId, false, httpRequest.getRemoteAddr());
        return Result.success(null);
    }

    @PutMapping("/orgs/{orgId}/items/{itemId}/two-factor")
    public Result<PassItemDetailVo> upsertSharedItemTwoFactor(
            @PathVariable Long orgId,
            @PathVariable Long itemId,
            @Valid @RequestBody UpsertPassItemTwoFactorRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(passBusinessService.upsertSharedItemTwoFactor(
                SecurityUtils.currentUserId(),
                orgId,
                itemId,
                request,
                httpRequest.getRemoteAddr()
        ));
    }

    @DeleteMapping("/orgs/{orgId}/items/{itemId}/two-factor")
    public Result<PassItemDetailVo> deleteSharedItemTwoFactor(
            @PathVariable Long orgId,
            @PathVariable Long itemId,
            HttpServletRequest httpRequest
    ) {
        return Result.success(passBusinessService.deleteSharedItemTwoFactor(
                SecurityUtils.currentUserId(),
                orgId,
                itemId,
                httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/orgs/{orgId}/items/{itemId}/two-factor/code")
    public Result<AuthenticatorCodeVo> generateSharedItemTwoFactorCode(
            @PathVariable Long orgId,
            @PathVariable Long itemId,
            HttpServletRequest httpRequest
    ) {
        return Result.success(passBusinessService.generateSharedItemTwoFactorCode(
                SecurityUtils.currentUserId(),
                orgId,
                itemId,
                httpRequest.getRemoteAddr()
        ));
    }

    @GetMapping("/orgs/{orgId}/incoming-item-shares")
    public Result<List<PassIncomingSharedItemSummaryVo>> listIncomingItemShares(
            @PathVariable Long orgId,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "false") Boolean favoriteOnly,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String itemType,
            HttpServletRequest httpRequest
    ) {
        return Result.success(passItemShareService.listIncomingSharedItems(
                SecurityUtils.currentUserId(),
                orgId,
                keyword,
                favoriteOnly,
                limit,
                itemType,
                httpRequest.getRemoteAddr()
        ));
    }

    @GetMapping("/orgs/{orgId}/incoming-item-shares/{itemId}")
    public Result<PassIncomingSharedItemDetailVo> getIncomingItemShare(
            @PathVariable Long orgId,
            @PathVariable Long itemId,
            HttpServletRequest httpRequest
    ) {
        return Result.success(passItemShareService.getIncomingSharedItem(
                SecurityUtils.currentUserId(),
                orgId,
                itemId,
                httpRequest.getRemoteAddr()
        ));
    }

    @PutMapping("/orgs/{orgId}/shared-vaults/{vaultId}/items/{itemId}")
    public Result<PassItemDetailVo> updateSharedItem(
            @PathVariable Long orgId,
            @PathVariable Long vaultId,
            @PathVariable Long itemId,
            @Valid @RequestBody UpdatePassItemRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(passBusinessService.updateSharedItem(
                SecurityUtils.currentUserId(),
                orgId,
                vaultId,
                itemId,
                request,
                httpRequest.getRemoteAddr()
        ));
    }

    @DeleteMapping("/orgs/{orgId}/shared-vaults/{vaultId}/items/{itemId}")
    public Result<Void> deleteSharedItem(
            @PathVariable Long orgId,
            @PathVariable Long vaultId,
            @PathVariable Long itemId,
            HttpServletRequest httpRequest
    ) {
        passBusinessService.deleteSharedItem(
                SecurityUtils.currentUserId(),
                orgId,
                vaultId,
                itemId,
                httpRequest.getRemoteAddr()
        );
        return Result.success(null);
    }

    @GetMapping("/orgs/{orgId}/activity")
    public Result<List<OrgAuditEventVo>> listActivity(
            @PathVariable Long orgId,
            @RequestParam(required = false) Integer limit,
            HttpServletRequest httpRequest
    ) {
        return Result.success(passBusinessService.listActivity(
                SecurityUtils.currentUserId(),
                orgId,
                limit,
                httpRequest.getRemoteAddr()
        ));
    }

    @GetMapping("/orgs/{orgId}/items/{itemId}/secure-links")
    public Result<List<PassSecureLinkVo>> listSecureLinks(
            @PathVariable Long orgId,
            @PathVariable Long itemId,
            HttpServletRequest httpRequest
    ) {
        return Result.success(passBusinessService.listSecureLinks(
                SecurityUtils.currentUserId(),
                orgId,
                itemId,
                publicBaseUrlResolver.resolve(httpRequest),
                httpRequest.getRemoteAddr()
        ));
    }

    @GetMapping("/orgs/{orgId}/secure-links")
    public Result<List<PassSecureLinkDashboardVo>> listOrgSecureLinks(
            @PathVariable Long orgId,
            HttpServletRequest httpRequest
    ) {
        return Result.success(passBusinessService.listOrgSecureLinks(
                SecurityUtils.currentUserId(),
                orgId,
                publicBaseUrlResolver.resolve(httpRequest),
                httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/orgs/{orgId}/items/{itemId}/secure-links")
    public Result<PassSecureLinkVo> createSecureLink(
            @PathVariable Long orgId,
            @PathVariable Long itemId,
            @Valid @RequestBody CreatePassSecureLinkRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(passBusinessService.createSecureLink(
                SecurityUtils.currentUserId(),
                orgId,
                itemId,
                request,
                publicBaseUrlResolver.resolve(httpRequest),
                httpRequest.getRemoteAddr()
        ));
    }

    @DeleteMapping("/orgs/{orgId}/secure-links/{linkId}")
    public Result<PassSecureLinkVo> revokeSecureLink(
            @PathVariable Long orgId,
            @PathVariable Long linkId,
            HttpServletRequest httpRequest
    ) {
        return Result.success(passBusinessService.revokeSecureLink(
                SecurityUtils.currentUserId(),
                orgId,
                linkId,
                publicBaseUrlResolver.resolve(httpRequest),
                httpRequest.getRemoteAddr()
        ));
    }
}
