package com.mmmail.server.commercial.oidc;

import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.commercial.FeatureCode;
import com.mmmail.server.model.vo.AuthResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Set;

@Service
public class OidcSsoService {

    private static final Set<String> DEFAULT_SCOPES = Set.of("openid", "email", "profile");

    private final OidcSsoDependencies dependencies;

    public OidcSsoService(OidcSsoDependencies dependencies) {
        this.dependencies = dependencies;
    }

    public OidcConfigVo config(Long orgId) {
        return dependencies.configRepository()
                .findByOrgId(orgId)
                .map(OidcConfigVo::from)
                .orElseGet(() -> OidcConfigVo.from(disabledConfig(orgId)));
    }

    public OidcConfigVo saveConfig(Long orgId, OidcConfigUpsertRequest request) {
        OidcClientConfig config = normalize(request.toConfig(orgId));
        dependencies.configRepository().save(config);
        return OidcConfigVo.from(config);
    }

    public OidcAuthorizationStartVo start(OidcAuthorizationStartRequest request) {
        OidcClientConfig config = requireEnabledConfig(request.orgId());
        return dependencies.stateService().start(config, request);
    }

    public OidcCallbackResult callback(String state, String code, String callbackUri, String ipAddress) {
        return dependencies.runtimeTraceService().observe("mmmail.oidc.callback", Map.of(
                "component", "oidc",
                "operation", "callback"
        ), () -> handleCallback(state, code, callbackUri, ipAddress));
    }

    private OidcCallbackResult handleCallback(String state, String code, String callbackUri, String ipAddress) {
        if (!StringUtils.hasText(code)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "OIDC authorization code is required");
        }
        OidcStateRecord record = dependencies.stateService().consume(state, callbackUri);
        requireCompleteState(record);
        OidcClientConfig config = requireEnabledConfig(record.orgId());
        OidcTokenResponse tokens = dependencies.tokenExchangeClient().exchange(config, record, code.trim());
        OidcUserIdentity identity = dependencies.idTokenValidator().validate(config, tokens, record.nonce());
        AuthResponse auth = dependencies.sessionIssuer().issue(new OidcSessionRequest(
                identity.subject(),
                identity.email(),
                ipAddress
        ));
        return new OidcCallbackResult(record.orgId(), record.state(), record.postLoginRedirectUri(), "authenticated", auth);
    }

    private OidcClientConfig requireEnabledConfig(Long orgId) {
        OidcClientConfig config = dependencies.configRepository()
                .findByOrgId(orgId)
                .orElseThrow(() -> new BizException(ErrorCode.ORG_NOT_FOUND, "OIDC config not found"));
        if (!config.enabled()) {
            throw new BizException(ErrorCode.FORBIDDEN, "OIDC SSO is not enabled for this organization");
        }
        dependencies.featureGate().requireFeature(
                dependencies.editionContextResolver().resolve(config.orgId()),
                FeatureCode.OIDC_SSO
        );
        return config;
    }

    private void requireCompleteState(OidcStateRecord record) {
        if (!StringUtils.hasText(record.nonce()) || !StringUtils.hasText(record.codeVerifier())) {
            throw new BizException(ErrorCode.SESSION_INVALID, "OIDC state is incomplete");
        }
    }

    private OidcClientConfig normalize(OidcClientConfig config) {
        if (!StringUtils.hasText(config.issuerUri()) || !StringUtils.hasText(config.clientId())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "OIDC issuer and client id are required");
        }
        if (!StringUtils.hasText(config.callbackUri())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "OIDC callback URI is required");
        }
        Set<String> scopes = config.scopes() == null || config.scopes().isEmpty()
                ? DEFAULT_SCOPES
                : Set.copyOf(config.scopes());
        return new OidcClientConfig(
                config.orgId(),
                config.enabled(),
                config.issuerUri().trim(),
                config.clientId().trim(),
                normalizeOptional(config.clientSecretRef()),
                config.callbackUri().trim(),
                scopes,
                config.allowedPostLoginRedirectUris() == null ? Set.of() : Set.copyOf(config.allowedPostLoginRedirectUris())
        );
    }

    private String normalizeOptional(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }

    private OidcClientConfig disabledConfig(Long orgId) {
        return new OidcClientConfig(orgId, false, "", "", "", "", DEFAULT_SCOPES, Set.of());
    }
}
