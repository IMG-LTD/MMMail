package com.mmmail.server.commercial.oidc;

import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class EnvironmentOidcClientSecretResolver implements OidcClientSecretResolver {

    private final Environment environment;

    public EnvironmentOidcClientSecretResolver(Environment environment) {
        this.environment = environment;
    }

    @Override
    public String resolve(String clientSecretRef) {
        if (!StringUtils.hasText(clientSecretRef)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "OIDC client secret ref is required");
        }
        String normalizedRef = clientSecretRef.trim();
        String secret = environment.getProperty(normalizedRef);
        if (!StringUtils.hasText(secret)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "OIDC client secret is not configured: " + normalizedRef);
        }
        return secret.trim();
    }
}
