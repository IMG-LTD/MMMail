package com.mmmail.server.commercial;

import java.util.Optional;

public interface LicenseStateRepository {

    void save(LicenseState state);

    Optional<LicenseState> findByOrgId(long orgId);
}
