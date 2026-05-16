package com.mmmail.server.commercial.oidc;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OidcTokenResponse(
        @JsonProperty("id_token") String idToken,
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("refresh_token") String refreshToken,
        @JsonProperty("expires_in") Long expiresIn,
        @JsonProperty("token_type") String tokenType
) {
}
