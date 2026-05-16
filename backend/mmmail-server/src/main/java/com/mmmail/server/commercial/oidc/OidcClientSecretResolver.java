package com.mmmail.server.commercial.oidc;

public interface OidcClientSecretResolver {

    String resolve(String clientSecretRef);
}
