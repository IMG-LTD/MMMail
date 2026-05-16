package com.mmmail.server.commercial.oidc;

import java.util.Optional;

public interface OidcConfigRepository {

    Optional<OidcClientConfig> findByOrgId(Long orgId);

    void save(OidcClientConfig config);
}
