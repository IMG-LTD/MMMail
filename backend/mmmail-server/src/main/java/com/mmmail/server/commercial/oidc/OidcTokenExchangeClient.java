package com.mmmail.server.commercial.oidc;

public interface OidcTokenExchangeClient {

    OidcTokenResponse exchange(OidcClientConfig config, OidcStateRecord state, String code);
}
