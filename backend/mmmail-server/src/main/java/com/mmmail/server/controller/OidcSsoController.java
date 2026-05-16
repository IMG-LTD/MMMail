package com.mmmail.server.controller;

import com.mmmail.common.model.Result;
import com.mmmail.server.commercial.FeatureCode;
import com.mmmail.server.commercial.oidc.OidcAuthorizationStartRequest;
import com.mmmail.server.commercial.oidc.OidcAuthorizationStartVo;
import com.mmmail.server.commercial.oidc.OidcCallbackResult;
import com.mmmail.server.commercial.oidc.OidcConfigUpsertRequest;
import com.mmmail.server.commercial.oidc.OidcConfigVo;
import com.mmmail.server.commercial.oidc.OidcSsoService;
import com.mmmail.server.security.AuthCookieService;
import com.mmmail.server.security.CommercialAuthorizationGate;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OidcSsoController {

    private final CommercialAuthorizationGate commercialAuthorizationGate;
    private final OidcSsoService oidcSsoService;
    private final AuthCookieService authCookieService;

    public OidcSsoController(
            CommercialAuthorizationGate commercialAuthorizationGate,
            OidcSsoService oidcSsoService,
            AuthCookieService authCookieService
    ) {
        this.commercialAuthorizationGate = commercialAuthorizationGate;
        this.oidcSsoService = oidcSsoService;
        this.authCookieService = authCookieService;
    }

    @GetMapping("/api/v2/orgs/{orgId}/oidc/config")
    public Result<OidcConfigVo> config(@PathVariable Long orgId, HttpServletRequest request) {
        commercialAuthorizationGate.enforceFeature(request, orgId, FeatureCode.OIDC_SSO);
        return Result.success(oidcSsoService.config(orgId));
    }

    @PutMapping("/api/v2/orgs/{orgId}/oidc/config")
    public Result<OidcConfigVo> saveConfig(
            @PathVariable Long orgId,
            @RequestBody OidcConfigUpsertRequest body,
            HttpServletRequest request
    ) {
        commercialAuthorizationGate.enforceFeature(request, orgId, FeatureCode.OIDC_SSO);
        return Result.success(oidcSsoService.saveConfig(orgId, body));
    }

    @PostMapping("/api/v2/auth/oidc/login")
    public Result<OidcAuthorizationStartVo> start(@RequestBody OidcAuthorizationStartRequest request) {
        return Result.success(oidcSsoService.start(request));
    }

    @GetMapping("/api/v2/auth/oidc/callback")
    public Result<OidcCallbackResult> callback(
            @RequestParam String state,
            @RequestParam String code,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        OidcCallbackResult result = oidcSsoService.callback(
                state,
                code,
                request.getRequestURL().toString(),
                request.getRemoteAddr()
        );
        authCookieService.attachAuthCookies(
                response,
                result.auth().refreshToken(),
                AuthCookieService.V2_AUTH_COOKIE_PATH
        );
        return Result.success(result);
    }
}
