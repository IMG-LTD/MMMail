package com.mmmail.server.commercial.oidc;

final class OidcIssuerUris {

    private static final String AUTH_PATH = "/protocol/openid-connect/auth";
    private static final String TOKEN_PATH = "/protocol/openid-connect/token";

    private OidcIssuerUris() {
    }

    static String authorizationEndpoint(String issuerUri) {
        return issuerBase(issuerUri) + AUTH_PATH;
    }

    static String tokenEndpoint(String issuerUri) {
        return issuerBase(issuerUri) + TOKEN_PATH;
    }

    static String issuerBase(String issuerUri) {
        String normalized = issuerUri.trim();
        if (normalized.endsWith("/")) {
            return normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
