package com.mmmail.server.commercial;

import java.util.Optional;

public interface SubscriptionStateRepository {

    void save(SubscriptionState state);

    Optional<SubscriptionState> findByOrgId(long orgId);
}
