package com.mmmail.server.commercial.oidc;

public interface OidcIdTokenValidator {

    OidcUserIdentity validate(OidcClientConfig config, OidcTokenResponse tokens, String nonce);
}
