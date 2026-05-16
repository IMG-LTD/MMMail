package com.mmmail.server.commercial.oidc;

import java.time.Instant;
import java.util.Optional;

public interface OidcStateRepository {

    void save(OidcStateRecord record);

    Optional<OidcStateRecord> findActive(String state, Instant now);

    void markConsumed(String state, Instant consumedAt);
}
