package com.mmmail.server.migration;

import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.configuration.FluentConfiguration;

public final class MigrationDefaults {

    public static final String BASELINE_VERSION = "1";
    public static final String[] LOCATIONS = {"classpath:db/migration"};

    private MigrationDefaults() {
    }

    public static FluentConfiguration apply(FluentConfiguration configuration) {
        return apply(configuration, LOCATIONS);
    }

    public static FluentConfiguration apply(FluentConfiguration configuration, String... locations) {
        return configuration
                .baselineOnMigrate(true)
                .baselineVersion(MigrationVersion.fromVersion(BASELINE_VERSION))
                .validateOnMigrate(true)
                .locations(locations);
    }
}
