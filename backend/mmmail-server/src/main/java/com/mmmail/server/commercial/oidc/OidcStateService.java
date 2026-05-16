package com.mmmail.server.commercial.oidc;

import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

@Service
public class OidcStateService {

    private static final long STATE_TTL_SECONDS = 600;
    private static final String CODE_CHALLENGE_METHOD = "S256";

    private final OidcStateRepository repository;
    private final Clock clock;
    private final Supplier<String> tokenSupplier;

    @Autowired
    public OidcStateService(OidcStateRepository repository) {
        this(repository, Clock.systemUTC(), SecureTokenGenerator::generate);
    }

    public OidcStateService(OidcStateRepository repository, Clock clock, Supplier<String> tokenSupplier) {
        this.repository = repository;
        this.clock = clock;
        this.tokenSupplier = tokenSupplier;
    }

    public OidcAuthorizationStartVo start(OidcClientConfig config, OidcAuthorizationStartRequest request) {
        requireEnabled(config);
        String postLoginRedirectUri = allowedPostLoginRedirect(config, request.postLoginRedirectUri());
        String state = tokenSupplier.get();
        String nonce = tokenSupplier.get();
        String codeVerifier = tokenSupplier.get();
        Instant expiresAt = clock.instant().plusSeconds(STATE_TTL_SECONDS);
        repository.save(new OidcStateRecord(
                state,
                config.orgId(),
                nonce,
                codeVerifier,
                config.callbackUri(),
                postLoginRedirectUri,
                expiresAt,
                null
        ));
        return new OidcAuthorizationStartVo(authorizationUrl(config, state, nonce, codeVerifier), state, expiresAt);
    }

    public OidcStateRecord consume(String state, String callbackUri) {
        if (!StringUtils.hasText(state)) {
            throw invalidState();
        }
        OidcStateRecord record = repository.findActive(state.trim(), clock.instant()).orElseThrow(this::invalidState);
        if (!record.callbackUri().equals(callbackUri)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "OIDC redirect URI mismatch");
        }
        repository.markConsumed(record.state(), clock.instant());
        return record;
    }

    private void requireEnabled(OidcClientConfig config) {
        if (config == null || !config.enabled()) {
            throw new BizException(ErrorCode.FORBIDDEN, "OIDC SSO is not enabled for this organization");
        }
    }

    private String allowedPostLoginRedirect(OidcClientConfig config, String redirectUri) {
        if (!StringUtils.hasText(redirectUri) || !config.allowedPostLoginRedirectUris().contains(redirectUri.trim())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "OIDC post-login redirect is not allowed");
        }
        return redirectUri.trim();
    }

    private String authorizationUrl(OidcClientConfig config, String state, String nonce, String codeVerifier) {
        return UriComponentsBuilder.fromUriString(OidcIssuerUris.authorizationEndpoint(config.issuerUri()))
                .queryParam("response_type", "code")
                .queryParam("client_id", config.clientId())
                .queryParam("redirect_uri", config.callbackUri())
                .queryParam("scope", scopes(config.scopes()))
                .queryParam("state", state)
                .queryParam("nonce", nonce)
                .queryParam("code_challenge", codeChallenge(codeVerifier))
                .queryParam("code_challenge_method", CODE_CHALLENGE_METHOD)
                .build()
                .encode()
                .toUriString();
    }

    private String scopes(Set<String> scopes) {
        List<String> ordered = new ArrayList<>();
        for (String preferred : List.of("openid", "email", "profile")) {
            if (scopes.contains(preferred)) {
                ordered.add(preferred);
            }
        }
        scopes.stream()
                .filter(scope -> !ordered.contains(scope))
                .sorted(Comparator.naturalOrder())
                .forEach(ordered::add);
        return String.join(" ", ordered);
    }

    private String codeChallenge(String codeVerifier) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 digest is unavailable", ex);
        }
    }

    private BizException invalidState() {
        return new BizException(ErrorCode.SESSION_INVALID, "OIDC state is invalid or expired");
    }
}
