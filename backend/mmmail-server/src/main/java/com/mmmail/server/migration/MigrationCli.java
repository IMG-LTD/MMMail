package com.mmmail.server.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.CoreErrorCode;
import org.flywaydb.core.api.output.InfoOutput;
import org.flywaydb.core.api.output.InfoResult;
import org.flywaydb.core.api.output.MigrateResult;
import org.flywaydb.core.api.output.ValidateOutput;
import org.flywaydb.core.api.output.ValidateResult;

import java.util.List;
import java.util.stream.Collectors;

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
            case "validate" -> validate(flyway, flyway.validateWithResult());
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

    private static void validate(Flyway flyway, ValidateResult result) {
        if (!result.validationSuccessful && !isPendingOnlyOnEmptySchema(result, flyway.info().getInfoResult())) {
            throw new IllegalStateException("Flyway validation failed: " + formatInvalidMigrations(result.invalidMigrations));
        }
        System.out.println("validation=ok");
    }

    static boolean isPendingOnlyOnEmptySchema(ValidateResult result, InfoResult info) {
        if (result.validationSuccessful || info == null || !info.allSchemasEmpty || info.schemaVersion != null) {
            return false;
        }
        if (result.invalidMigrations == null || result.invalidMigrations.isEmpty()) {
            return false;
        }
        return result.invalidMigrations.stream()
                .map(output -> output.errorDetails == null ? null : output.errorDetails.errorCode)
                .allMatch(errorCode -> errorCode == CoreErrorCode.RESOLVED_VERSIONED_MIGRATION_NOT_APPLIED
                        || errorCode == CoreErrorCode.RESOLVED_REPEATABLE_MIGRATION_NOT_APPLIED);
    }

    static String formatInvalidMigrations(List<ValidateOutput> invalidMigrations) {
        if (invalidMigrations == null || invalidMigrations.isEmpty()) {
            return "unknown";
        }
        return invalidMigrations.stream()
                .map(MigrationCli::formatInvalidMigration)
                .collect(Collectors.joining("; "));
    }

    private static String formatInvalidMigration(ValidateOutput output) {
        String version = output.version == null ? "?" : output.version;
        String description = output.description == null ? "?" : output.description;
        String code = output.errorDetails == null || output.errorDetails.errorCode == null
                ? "UNKNOWN"
                : output.errorDetails.errorCode.toString();
        String message = output.errorDetails == null || output.errorDetails.errorMessage == null
                ? "no details"
                : output.errorDetails.errorMessage;
        return "%s:%s [%s] %s".formatted(version, description, code, message);
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
