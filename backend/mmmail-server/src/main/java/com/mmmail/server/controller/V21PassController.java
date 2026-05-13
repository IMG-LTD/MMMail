package com.mmmail.server.controller;

import com.mmmail.common.model.Result;
import com.mmmail.server.model.dto.CreatePassItemRequest;
import com.mmmail.server.model.dto.UpdatePassAliasRequest;
import com.mmmail.server.model.dto.UpdatePassItemRequest;
import com.mmmail.server.model.dto.V21PassSecureLinkQuery;
import com.mmmail.server.model.dto.V21PassSecureLinkRequest;
import com.mmmail.server.model.vo.PassItemSummaryVo;
import com.mmmail.server.model.vo.PassMailAliasVo;
import com.mmmail.server.model.vo.PassMonitorOverviewVo;
import com.mmmail.server.model.vo.PassSecureLinkDashboardVo;
import com.mmmail.server.model.vo.PassSecureLinkVo;
import com.mmmail.server.model.vo.V21PassVaultVo;
import com.mmmail.server.security.JwtPrincipal;
import com.mmmail.server.service.PublicBaseUrlResolver;
import com.mmmail.server.service.V21PassRuntimeBridgeService;
import com.mmmail.server.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v2/pass")
public class V21PassController {

    private final V21PassRuntimeBridgeService passRuntimeBridgeService;
    private final PublicBaseUrlResolver publicBaseUrlResolver;

    public V21PassController(
            V21PassRuntimeBridgeService passRuntimeBridgeService,
            PublicBaseUrlResolver publicBaseUrlResolver
    ) {
        this.passRuntimeBridgeService = passRuntimeBridgeService;
        this.publicBaseUrlResolver = publicBaseUrlResolver;
    }

    @GetMapping("/vaults")
    public Result<List<V21PassVaultVo>> vaults() {
        JwtPrincipal principal = SecurityUtils.currentPrincipal();
        return Result.success(passRuntimeBridgeService.listVaults(principal.userId(), principal.email()));
    }

    @GetMapping("/items")
    public Result<List<PassItemSummaryVo>> items(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "false") Boolean favoriteOnly,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String itemType
    ) {
        return Result.success(passRuntimeBridgeService.listItems(
                SecurityUtils.currentUserId(),
                keyword,
                favoriteOnly,
                limit,
                itemType
        ));
    }

    @PostMapping("/items")
    public Result<PassItemSummaryVo> createItem(
            @Valid @RequestBody CreatePassItemRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(passRuntimeBridgeService.createItem(
                SecurityUtils.currentUserId(),
                request,
                ip(httpRequest)
        ));
    }

    @PatchMapping("/items/{itemId}")
    public Result<PassItemSummaryVo> updateItem(
            @PathVariable String itemId,
            @Valid @RequestBody UpdatePassItemRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(passRuntimeBridgeService.updateItem(
                SecurityUtils.currentUserId(),
                itemId,
                request,
                ip(httpRequest)
        ));
    }

    @PostMapping("/share")
    public Result<PassSecureLinkVo> share(
            @Valid @RequestBody V21PassSecureLinkRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(passRuntimeBridgeService.createSecureLink(
                SecurityUtils.currentUserId(),
                request,
                publicBaseUrlResolver.resolve(httpRequest),
                ip(httpRequest)
        ));
    }

    @GetMapping("/secure-links")
    public Result<List<PassSecureLinkDashboardVo>> secureLinks(
            @Valid @ModelAttribute V21PassSecureLinkQuery query,
            HttpServletRequest httpRequest
    ) {
        return Result.success(passRuntimeBridgeService.listSecureLinks(
                SecurityUtils.currentUserId(),
                query.orgId(),
                publicBaseUrlResolver.resolve(httpRequest),
                ip(httpRequest)
        ));
    }

    @PostMapping("/secure-links")
    public Result<PassSecureLinkVo> createSecureLink(
            @Valid @RequestBody V21PassSecureLinkRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(passRuntimeBridgeService.createSecureLink(
                SecurityUtils.currentUserId(),
                request,
                publicBaseUrlResolver.resolve(httpRequest),
                ip(httpRequest)
        ));
    }

    @DeleteMapping("/secure-links/{linkId}")
    public Result<Void> deleteSecureLink(
            @PathVariable String linkId,
            @Valid @ModelAttribute V21PassSecureLinkQuery query,
            HttpServletRequest httpRequest
    ) {
        passRuntimeBridgeService.revokeSecureLink(
                SecurityUtils.currentUserId(),
                query.orgId(),
                linkId,
                publicBaseUrlResolver.resolve(httpRequest),
                ip(httpRequest)
        );
        return Result.success(null);
    }

    @GetMapping("/aliases")
    public Result<List<PassMailAliasVo>> aliases(HttpServletRequest httpRequest) {
        return Result.success(passRuntimeBridgeService.listAliases(
                SecurityUtils.currentUserId(),
                ip(httpRequest)
        ));
    }

    @PatchMapping("/aliases/{aliasId}")
    public Result<PassMailAliasVo> updateAlias(
            @PathVariable String aliasId,
            @Valid @RequestBody UpdatePassAliasRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(passRuntimeBridgeService.updateAlias(
                SecurityUtils.currentUserId(),
                aliasId,
                request,
                ip(httpRequest)
        ));
    }

    @GetMapping("/monitor")
    public Result<PassMonitorOverviewVo> monitor(HttpServletRequest httpRequest) {
        return Result.success(passRuntimeBridgeService.readMonitor(
                SecurityUtils.currentUserId(),
                ip(httpRequest)
        ));
    }

    private String ip(HttpServletRequest httpRequest) {
        return httpRequest.getRemoteAddr();
    }
}
