package com.mmmail.server.commercial.oidc;

import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DefaultOidcIdTokenValidator implements OidcIdTokenValidator {

    @Override
    public OidcUserIdentity validate(OidcClientConfig config, OidcTokenResponse tokens, String nonce) {
        if (tokens == null || !StringUtils.hasText(tokens.idToken())) {
            throw new BizException(ErrorCode.SESSION_INVALID, "OIDC id_token is required");
        }
        Jwt jwt = NimbusJwtDecoder.withIssuerLocation(config.issuerUri()).build().decode(tokens.idToken());
        validateIssuer(config, jwt);
        validateAudience(config, jwt);
        validateNonce(jwt, nonce);
        return identity(jwt);
    }

    private void validateIssuer(OidcClientConfig config, Jwt jwt) {
        String expectedIssuer = OidcIssuerUris.issuerBase(config.issuerUri());
        if (jwt.getIssuer() == null || !expectedIssuer.equals(jwt.getIssuer().toString())) {
            throw new BizException(ErrorCode.SESSION_INVALID, "OIDC id_token issuer mismatch");
        }
    }

    private void validateAudience(OidcClientConfig config, Jwt jwt) {
        if (!jwt.getAudience().contains(config.clientId())) {
            throw new BizException(ErrorCode.SESSION_INVALID, "OIDC id_token audience mismatch");
        }
    }

    private void validateNonce(Jwt jwt, String nonce) {
        String actualNonce = jwt.getClaimAsString("nonce");
        if (!StringUtils.hasText(actualNonce) || !actualNonce.equals(nonce)) {
            throw new BizException(ErrorCode.SESSION_INVALID, "OIDC id_token nonce mismatch");
        }
    }

    private OidcUserIdentity identity(Jwt jwt) {
        String subject = jwt.getSubject();
        String email = jwt.getClaimAsString("email");
        if (!StringUtils.hasText(subject) || !StringUtils.hasText(email)) {
            throw new BizException(ErrorCode.SESSION_INVALID, "OIDC id_token subject and email are required");
        }
        return new OidcUserIdentity(subject, email.trim().toLowerCase());
    }
}
