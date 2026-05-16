package com.mmmail.server.commercial.oidc;

import com.mmmail.server.commercial.EditionContextResolver;
import com.mmmail.server.commercial.FeatureGate;
import com.mmmail.server.observability.RuntimeTraceService;
import org.springframework.stereotype.Component;

@Component
public record OidcSsoDependencies(
        OidcConfigRepository configRepository,
        OidcStateService stateService,
        EditionContextResolver editionContextResolver,
        FeatureGate featureGate,
        RuntimeTraceService runtimeTraceService,
        OidcTokenExchangeClient tokenExchangeClient,
        OidcIdTokenValidator idTokenValidator,
        OidcSessionIssuer sessionIssuer
) {
}
