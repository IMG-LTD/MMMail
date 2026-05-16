package com.mmmail.server.commercial.oidc;

import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Service
public class RestClientOidcTokenExchangeClient implements OidcTokenExchangeClient {

    private static final String AUTHORIZATION_CODE_GRANT = "authorization_code";

    private final RestClient restClient;
    private final OidcClientSecretResolver secretResolver;

    public RestClientOidcTokenExchangeClient(
            RestClient.Builder restClientBuilder,
            OidcClientSecretResolver secretResolver
    ) {
        this.restClient = restClientBuilder.build();
        this.secretResolver = secretResolver;
    }

    @Override
    public OidcTokenResponse exchange(OidcClientConfig config, OidcStateRecord state, String code) {
        requireExchangeInput(state, code);
        OidcTokenResponse response = restClient.post()
                .uri(OidcIssuerUris.tokenEndpoint(config.issuerUri()))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(tokenRequest(config, state, code.trim()))
                .retrieve()
                .body(OidcTokenResponse.class);
        if (response == null || !StringUtils.hasText(response.idToken())) {
            throw new BizException(ErrorCode.SESSION_INVALID, "OIDC token response did not include id_token");
        }
        return response;
    }

    private MultiValueMap<String, String> tokenRequest(OidcClientConfig config, OidcStateRecord state, String code) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", AUTHORIZATION_CODE_GRANT);
        form.add("code", code);
        form.add("redirect_uri", state.callbackUri());
        form.add("client_id", config.clientId());
        form.add("client_secret", secretResolver.resolve(config.clientSecretRef()));
        form.add("code_verifier", state.codeVerifier());
        return form;
    }

    private void requireExchangeInput(OidcStateRecord state, String code) {
        if (state == null || !StringUtils.hasText(state.codeVerifier())) {
            throw new BizException(ErrorCode.SESSION_INVALID, "OIDC state is incomplete");
        }
        if (!StringUtils.hasText(code)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "OIDC authorization code is required");
        }
    }
}
