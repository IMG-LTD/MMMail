package com.mmmail.server.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.InfoOutput;
import org.flywaydb.core.api.output.InfoResult;
import org.flywaydb.core.api.output.MigrateResult;
import org.flywaydb.core.api.output.ValidateResult;

import java.util.List;

public final class MigrationCli {

    private static final String URL_PROPERTY = "mmmail.migration.datasource.url";
    private static final String USERNAME_PROPERTY = "mmmail.migration.datasource.username";
    private static final String PASSWORD_PROPERTY = "mmmail.migration.datasource.password";

    private MigrationCli() {
    }

    public static void main(String[] args) {
        String command = args.length == 0 ? "migrate" : args[0];
        Flyway flyway = createFlyway();
        switch (command) {
            case "info" -> printInfo(flyway.info().getInfoResult());
            case "validate" -> validate(flyway.validateWithResult());
            case "migrate" -> migrate(flyway.migrate());
            case "repair" -> repair(flyway);
            default -> throw new IllegalArgumentException("Unsupported migration command: " + command);
        }
    }

    private static Flyway createFlyway() {
        String url = requireSetting("SPRING_DATASOURCE_URL", URL_PROPERTY);
        String username = requireSetting("SPRING_DATASOURCE_USERNAME", USERNAME_PROPERTY);
        String password = requireSetting("SPRING_DATASOURCE_PASSWORD", PASSWORD_PROPERTY);
        return MigrationDefaults.apply(Flyway.configure().dataSource(url, username, password)).load();
    }

    private static String requireSetting(String envKey, String propertyKey) {
        String value = System.getProperty(propertyKey);
        if (value != null && !value.isBlank()) {
            return value;
        }
        value = System.getenv(envKey);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required datasource setting: " + envKey + " or -D" + propertyKey);
        }
        return value;
    }

    private static void migrate(MigrateResult result) {
        System.out.printf("migrations=%d target=%s%n", result.migrationsExecuted, result.targetSchemaVersion);
    }

    private static void validate(ValidateResult result) {
        if (!result.validationSuccessful) {
            throw new IllegalStateException("Flyway validation failed: " + result.invalidMigrations);
        }
        System.out.println("validation=ok");
    }

    private static void repair(Flyway flyway) {
        flyway.repair();
        System.out.println("repair=ok");
    }

    private static void printInfo(InfoResult result) {
        List<InfoOutput> migrations = result.migrations == null ? List.of() : result.migrations;
        long pending = migrations.stream()
                .filter(migration -> "Pending".equalsIgnoreCase(migration.state))
                .count();
        long applied = migrations.stream()
                .filter(migration -> migration.version != null && !"Pending".equalsIgnoreCase(migration.state))
                .count();
        System.out.printf("current=%s pending=%d applied=%d%n",
                result.schemaVersion == null ? "none" : result.schemaVersion,
                pending,
                applied);
    }
}
